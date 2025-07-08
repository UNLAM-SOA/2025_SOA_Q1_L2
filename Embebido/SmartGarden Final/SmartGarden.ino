#include <WiFi.h>
#include <PubSubClient.h>
#include <Wire.h>
#include "Constantes.h"
#include "rgb_lcd.h"

rgb_lcd lcd;
#define MODO_DEBUG true

WiFiClient espClient;
PubSubClient client(espClient);

// === Tipos y estructuras ===
enum tipo_evento_t {
  TIPO_EVENTO_CONTINUE,
  TIPO_EVENTO_TIMEOUT_RIEGO,
  TIPO_PUSH_BUTTON,
  TIPO_EVENTO_HUMEDAD_BAJA_Y_ES_DE_NOCHE,
  TIPO_EVENTO_HUMEDAD_OK,
  TIPO_EVENTO_SENSOR_ERROR
};

enum estado_t {
  ESTADO_INIT,
  ESTADO_IDLE,
  ESTADO_REGANDO_MANUAL,
  ESTADO_REGANDO_AUTOMATICO,
  ESTADO_TERMINANDO_RIEGO,
  ESTADO_ERROR_SENSOR
};

struct stEvento {
  tipo_evento_t tipo;
};

// === Variables globales ===
estado_t estado = ESTADO_INIT;
stEvento evento;
QueueHandle_t queueEvents;
TaskHandle_t loopTaskHandler;
TaskHandle_t loopNewEventHandler;

#define HUMEDAD_UMBRAL_MIN  (ADC_MAX_VALUE * HUMEDAD_UMBRAL_MIN_PCT / 100)
#define HUMEDAD_UMBRAL_MAX  (ADC_MAX_VALUE * HUMEDAD_UMBRAL_MAX_PCT / 100)

short indice_evento = 0;
long tiempo_ultimo_evento = 0;
int humedad_previa = 0;
int humedad_actual = 0;
int humedad = 0;
int luz = 0;
int ultimo_envio_datos = 0;
unsigned long tiempo_inicio_riego = 0;
bool timeout_habilitado = false;
bool sensor_historico_valido = true;
int intentos_sensor_sin_cambio = 0;

// === Funciones auxiliares ===
void debug_print(const char* msg) {
  if (MODO_DEBUG) Serial.println(msg);
}

void debug_println(const String& msg) {
  if (MODO_DEBUG) Serial.println(msg);
}

void mostrar_lcd(const char* mensaje) {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(mensaje);
  debug_print("[LCD] ");
  debug_print(mensaje);
}

int leer_humedad() {
  humedad = 4095 - analogRead(GPIO_SENSOR_HUMEDAD);
  return humedad;
}

int leer_luz() {
  luz = analogRead(GPIO_SENSOR_LUZ);
  return luz;
}

void enviar_datos_mqtt(int humedadubi, int luzubi) {
  String h_str = String(humedadubi);
  String l_str = String(luzubi);
  client.publish(TOPIC_HUMEDAD, h_str.c_str());
  client.publish(TOPIC_LUZ, l_str.c_str());
  debug_println("Publicado humedad: " + h_str);
  debug_println("Publicado luz: " + l_str);
}

bool sensores_validos() {
  return leer_humedad() >= MINIMO_HUMEDAD_VALIDA && leer_luz() >= MINIMO_LUZ_VALIDA;
}

bool humedad_baja() {
  return leer_humedad() < HUMEDAD_UMBRAL_MIN;
}

bool humedad_alta() {
  return leer_humedad() >= HUMEDAD_UMBRAL_MAX;
}

bool es_noche() {
  return leer_luz() < LUZ_UMBRAL;
}

bool boton_presionado() {
  return digitalRead(GPIO_BOTON) == LOW;
}

void encender_bomba_y_buzzer() {
  digitalWrite(GPIO_RELE, HIGH);
  digitalWrite(GPIO_BUZZER, HIGH);
  debug_print("[ACCION] Bomba y buzzer encendidos");
}

void apagar_bomba_y_buzzer() {
  digitalWrite(GPIO_RELE, LOW);
  digitalWrite(GPIO_BUZZER, LOW);
  debug_print("[ACCION] Bomba y buzzer apagados");
}

// === Deteccion de eventos ===
typedef bool (*func_evento_t)(stEvento*, long);

bool detectar_sensor_error(stEvento* e, long t) {
  if (!sensores_validos()) {
    e->tipo = TIPO_EVENTO_SENSOR_ERROR;
    return true;
  }
  return false;
}

bool detectar_push_button(stEvento* e, long t) {
  if (boton_presionado()) {
    e->tipo = TIPO_PUSH_BUTTON;
    return true;
  }
  return false;
}

bool detectar_auto_riego(stEvento* e, long t) {
  if (estado == ESTADO_IDLE && sensor_historico_valido && humedad_baja() && es_noche()) {
    e->tipo = TIPO_EVENTO_HUMEDAD_BAJA_Y_ES_DE_NOCHE;
    return true;
  }
  return false;
}

bool detectar_timeout(stEvento* e, long t) {
  if (timeout_habilitado && millis() - tiempo_inicio_riego > TIEMPO_RIEGO_MS) {
    e->tipo = TIPO_EVENTO_TIMEOUT_RIEGO;
    timeout_habilitado = false;
    return true;
  }
  return false;
}

bool detectar_humedad_ok(stEvento* e, long t) {
  if (estado == ESTADO_REGANDO_AUTOMATICO && humedad_alta()) {
    e->tipo = TIPO_EVENTO_HUMEDAD_OK;
    return true;
  }
  return false;
}

func_evento_t eventos_posibles[MAX_EVENTOS] = {
  detectar_sensor_error,
  detectar_push_button,
  detectar_auto_riego,
  detectar_timeout,
  detectar_humedad_ok
};

void generar_evento() {
  long ahora = millis();
  if ((ahora - tiempo_ultimo_evento) > INTERVALO_TAREAS) {
    tiempo_ultimo_evento = ahora;
    if (eventos_posibles[indice_evento](&evento, ahora)) {
      indice_evento = (indice_evento + 1) % MAX_EVENTOS;
      xQueueSend(queueEvents, &evento, portMAX_DELAY);
      return;
    }
    indice_evento = (indice_evento + 1) % MAX_EVENTOS;
  }
  evento.tipo = TIPO_EVENTO_CONTINUE;
  xQueueSend(queueEvents, &evento, portMAX_DELAY);
}

// === Maquina de estados ===
void maquina_estados() {
  stEvento ev;
  xQueueReceive(queueEvents, &ev, portMAX_DELAY);

  switch (estado) {
    case ESTADO_INIT:
      if (ev.tipo == TIPO_EVENTO_CONTINUE) {
        mostrar_lcd("Sistema listo");
        estado = ESTADO_IDLE;
      }
      break;

    case ESTADO_IDLE:
      switch (ev.tipo) {
        case TIPO_PUSH_BUTTON:
          encender_bomba_y_buzzer();
          mostrar_lcd("Riego manual");
          tiempo_inicio_riego = millis();
          sensor_historico_valido = true;
          timeout_habilitado = true;
          estado = ESTADO_REGANDO_MANUAL;
          client.publish(TOPIC_RIEGO_ESTADO, "1");
          break;
        case TIPO_EVENTO_HUMEDAD_BAJA_Y_ES_DE_NOCHE:
          encender_bomba_y_buzzer();
          mostrar_lcd("Riego automatico");
          tiempo_inicio_riego = millis();
          humedad_previa = leer_humedad();
          timeout_habilitado = true;
          client.publish(TOPIC_RIEGO_ESTADO, "1");
          estado = ESTADO_REGANDO_AUTOMATICO;
          break;
        case TIPO_EVENTO_SENSOR_ERROR:
          mostrar_lcd("Error sensores");
          estado = ESTADO_ERROR_SENSOR;
          break;
        default:
          if (millis() - ultimo_envio_datos >= INTERVALO_ENVIO_UBIDOTS) {
            enviar_datos_mqtt(humedad, luz);
            ultimo_envio_datos = millis();
          }
          break;
      }
      break;

    case ESTADO_REGANDO_MANUAL:
      switch (ev.tipo) {
        case TIPO_EVENTO_TIMEOUT_RIEGO:
        case TIPO_PUSH_BUTTON:
          mostrar_lcd("Finalizando riego");
          estado = ESTADO_TERMINANDO_RIEGO;
          break;
        case TIPO_EVENTO_SENSOR_ERROR:
          mostrar_lcd("Error sensores");
          estado = ESTADO_ERROR_SENSOR;
          break;
        default:
          if (millis() - ultimo_envio_datos >= INTERVALO_ENVIO_UBIDOTS) {
            enviar_datos_mqtt(humedad, luz);
            ultimo_envio_datos = millis();
          }
          break;
      }
      break;

    case ESTADO_REGANDO_AUTOMATICO:
      switch (ev.tipo) {
        case TIPO_EVENTO_TIMEOUT_RIEGO:
          humedad_actual = leer_humedad();
          if (abs(humedad_actual - humedad_previa) < UMBRAL_CAMBIO_MINIMO) {
            intentos_sensor_sin_cambio++;
            if (intentos_sensor_sin_cambio >= MAX_INTENTOS_RIEGO) {
              sensor_historico_valido = false;
              mostrar_lcd("Error en el sensor");
            }
          } else {
            intentos_sensor_sin_cambio = 0;
            sensor_historico_valido = true;
          }
          mostrar_lcd("Finalizando RO");
          estado = ESTADO_TERMINANDO_RIEGO;
          break;
        case TIPO_EVENTO_HUMEDAD_OK:
        case TIPO_PUSH_BUTTON:
          mostrar_lcd("Finalizando...");
          estado = ESTADO_TERMINANDO_RIEGO;
          break;
        case TIPO_EVENTO_SENSOR_ERROR:
          mostrar_lcd("Error sensores");
          estado = ESTADO_ERROR_SENSOR;
          break;
        default:
          if (millis() - ultimo_envio_datos >= INTERVALO_ENVIO_UBIDOTS) {
            enviar_datos_mqtt(humedad, luz);
            ultimo_envio_datos = millis();
          }
          break;
      }
      break;

    case ESTADO_TERMINANDO_RIEGO:
      apagar_bomba_y_buzzer();
      mostrar_lcd("Listo!");
      client.publish(TOPIC_RIEGO_ESTADO, "0");
      estado = ESTADO_IDLE;
      break;

    case ESTADO_ERROR_SENSOR:
      apagar_bomba_y_buzzer();
      debug_print("[ESTADO] ERROR (sin cambio)");
      break;
  }
}

// === FreeRTOS ===
void vLoopTask(void *pvParameters) {
  while (1) {
    maquina_estados();
  }
}

void vGetNewEventTask(void *pvParameters) {
  while (1) {
    generar_evento();
    vTaskDelay(pdMS_TO_TICKS(INTERVALO_TAREAS));
  }
}

void vMQTTLoopTask(void *pvParameters) {
  while (1) {
    client.loop();
    vTaskDelay(pdMS_TO_TICKS(10));
  }
}

// === Callback MQTT ===
void callback(char* topic, byte* payload, unsigned int length) {
  String mensaje;
  for (unsigned int i = 0; i < length; i++) {
    mensaje += (char)payload[i];
  }

  if (String(topic) == TOPIC_RIEGO_CONTROL) {
    debug_println("[ORDEN REMOTA] Activando riego manual");
    stEvento eventoRemoto;
    eventoRemoto.tipo = TIPO_PUSH_BUTTON;
    xQueueSend(queueEvents, &eventoRemoto, portMAX_DELAY);
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(GPIO_RELE, OUTPUT);
  pinMode(GPIO_BOTON, INPUT_PULLUP);
  pinMode(GPIO_BUZZER, OUTPUT);
  pinMode(GPIO_SENSOR_HUMEDAD, INPUT);
  pinMode(GPIO_SENSOR_LUZ, INPUT);

  Wire.begin(GPIO_LCD_SDA, GPIO_LCD_SCL);
  lcd.begin(COLUMNAS_LCD, FILAS_LCD);
  mostrar_lcd("Iniciando...");

  queueEvents = xQueueCreate(QUEUE_SIZE, sizeof(stEvento));

  xTaskCreate(vLoopTask, "LoopFSM", STACK_SIZE, NULL, PRIORIDAD_TAREAS, &loopTaskHandler);
  xTaskCreate(vGetNewEventTask, "EventTask", STACK_SIZE, NULL, PRIORIDAD_TAREAS, &loopNewEventHandler);
  xTaskCreate(vMQTTLoopTask, "MQTTLoop", STACK_SIZE, NULL, PRIORIDAD_TAREAS, NULL);

  WiFi.begin(WIFISSID, PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi conectado");

  client.setServer(MQTT_BROKER, 1885);
  client.setCallback(callback);

  while (!client.connected()) {
    String clientId = "ESP32Client-" + String(random(0xffff), HEX);

    if (client.connect(clientId.c_str(), MQTT_USER, MQTT_PASSWORD)) {
      Serial.println("Conectado a Mosquitto");
    } else {
      Serial.print("Fallo la conexion, rc=");
      Serial.print(client.state());
      delay(5000);
    }
  }

  client.subscribe(TOPIC_RIEGO_CONTROL);
  Serial.println("Suscripto a: " + String(TOPIC_RIEGO_CONTROL));
}

void loop() {
  // No se usa. Usamos FreeRTOS
}
