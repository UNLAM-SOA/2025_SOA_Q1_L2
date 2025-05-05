#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include "Constantes.h"

#define MODO_DEBUG true

//--------------------------------------------------
// Definiciones de tipos y estructuras
//--------------------------------------------------
enum tipo_evento_t {
  TIPO_EVENTO_CONTINUE,
  TIPO_EVENTO_TIMEOUT_RIEGO,
  TIPO_EVENTO_USUARIO_PIDE_REGAR,
  TIPO_EVENTO_USUARIO_PIDE_FINALIZAR,
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

//--------------------------------------------------
// Variables globales
//--------------------------------------------------
estado_t estado = ESTADO_INIT;
stEvento evento;
unsigned long tiempo_inicio_riego = 0;

QueueHandle_t queueEvents;
TaskHandle_t loopTaskHandler;
TaskHandle_t loopNewEventHandler;

#define HUMEDAD_UMBRAL_MIN  (ADC_MAX_VALUE * HUMEDAD_UMBRAL_MIN_PCT / 100)
#define HUMEDAD_UMBRAL_MAX  (ADC_MAX_VALUE * HUMEDAD_UMBRAL_MAX_PCT / 100)

LiquidCrystal_I2C lcd(0x27, COLUMNAS_LCD, FILAS_LCD);

short indice_evento = 0;
long tiempo_ultimo_evento = 0;


//--------------------------------------------------
// Funciones de debug y LCD
//--------------------------------------------------
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

//--------------------------------------------------
// Funciones de hardware y sensores
//--------------------------------------------------
int leer_humedad() {
  return analogRead(GPIO_SENSOR_HUMEDAD);
}

int leer_luz() {
  return analogRead(GPIO_SENSOR_LUZ);
}

bool sensores_validos() {
  return leer_humedad() >= MINIMO_HUMEDAD_VALIDA && leer_luz() >= MINIMO_LUZ_VALIDA;
}

bool humedad_baja() {
  return leer_humedad() < HUMEDAD_UMBRAL_MIN;
}

bool es_noche() {
  return leer_luz() < LUZ_UMBRAL;
}

bool humedad_alta() {
  return leer_humedad() >= HUMEDAD_UMBRAL_MAX;
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

//--------------------------------------------------
// Funciones de deteccion de eventos, para que podamos detectar los eventos sin darle prioridad a un sensor
//--------------------------------------------------
typedef bool (*func_evento_t)(stEvento*, long);

bool detectar_sensor_error(stEvento* e, long t) {
  if (!sensores_validos()) {
    e->tipo = TIPO_EVENTO_SENSOR_ERROR;
    return true;
  }
  return false;
}

bool detectar_boton_presionado(stEvento* e, long t) {
  if (boton_presionado()) {
    e->tipo = (estado == ESTADO_IDLE) ? TIPO_EVENTO_USUARIO_PIDE_REGAR : TIPO_EVENTO_USUARIO_PIDE_FINALIZAR;
    return true;
  }
  return false;
}

bool detectar_auto_riego(stEvento* e, long t) {
  if (estado == ESTADO_IDLE && humedad_baja() && es_noche()) {
    e->tipo = TIPO_EVENTO_HUMEDAD_BAJA_Y_ES_DE_NOCHE;
    return true;
  }
  return false;
}

bool detectar_timeout(stEvento* e, long t) {
  if ((estado == ESTADO_REGANDO_MANUAL || estado == ESTADO_REGANDO_AUTOMATICO) && millis() - tiempo_inicio_riego > TIEMPO_RIEGO_MS) {
    e->tipo = TIPO_EVENTO_TIMEOUT_RIEGO;
    return true;
  }
  return false;
}

bool detectar_humedad_alta(stEvento* e, long t) {
  if (estado == ESTADO_REGANDO_AUTOMATICO && humedad_alta()) {
    e->tipo = TIPO_EVENTO_HUMEDAD_OK;
    return true;
  }
  return false;
}

func_evento_t eventos_posibles[MAX_EVENTOS] = {
  detectar_sensor_error,
  detectar_boton_presionado,
  detectar_auto_riego,
  detectar_timeout,
  detectar_humedad_alta
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

//--------------------------------------------------
// Maquina de estados
//--------------------------------------------------
void maquina_estados() {
  stEvento ev;
  xQueueReceive(queueEvents, &ev, portMAX_DELAY);

  switch (estado) {
    case ESTADO_INIT:
      if (ev.tipo == TIPO_EVENTO_CONTINUE) {
        mostrar_lcd("Sistema listo");
        estado = ESTADO_IDLE;
        debug_print("[ESTADO] -> ESPERA");
      }
      break;

    case ESTADO_IDLE:
      switch (ev.tipo) {
        case TIPO_EVENTO_USUARIO_PIDE_REGAR:
        case TIPO_EVENTO_HUMEDAD_BAJA_Y_ES_DE_NOCHE:
          encender_bomba_y_buzzer();
          mostrar_lcd(ev.tipo == TIPO_EVENTO_USUARIO_PIDE_REGAR ? "Riego manual..." : "Riego automatico");
          tiempo_inicio_riego = millis();
          estado = (ev.tipo == TIPO_EVENTO_USUARIO_PIDE_REGAR) ? ESTADO_REGANDO_MANUAL : ESTADO_REGANDO_AUTOMATICO;
          break;
        case TIPO_EVENTO_SENSOR_ERROR:
          mostrar_lcd("Error sensores");
          estado = ESTADO_ERROR_SENSOR;
          break;
        default:
          break;
      }
      break;

    case ESTADO_REGANDO_MANUAL:
    case ESTADO_REGANDO_AUTOMATICO:
      switch (ev.tipo) {
        case TIPO_EVENTO_TIMEOUT_RIEGO:
        case TIPO_EVENTO_USUARIO_PIDE_FINALIZAR:
        case TIPO_EVENTO_HUMEDAD_OK:
          mostrar_lcd("Finalizando...");
          estado = ESTADO_TERMINANDO_RIEGO;
          break;
        case TIPO_EVENTO_SENSOR_ERROR:
          mostrar_lcd("Error sensores");
          estado = ESTADO_ERROR_SENSOR;
          break;
        default:
          break;
      }
      break;

    case ESTADO_TERMINANDO_RIEGO:
      apagar_bomba_y_buzzer();
      mostrar_lcd("Listo!");
      estado = ESTADO_IDLE;
      break;

    case ESTADO_ERROR_SENSOR:
      apagar_bomba_y_buzzer();
      debug_print("[ESTADO] ERROR (sin cambio)");
      break;
  }
}

//--------------------------------------------------
// Tareas de FreeRTOS
//--------------------------------------------------
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

//--------------------------------------------------
// Inicializacion
//--------------------------------------------------
void inicializar() {
  Serial.begin(115200);
  pinMode(GPIO_RELE, OUTPUT);
  pinMode(GPIO_BOTON, INPUT_PULLUP);
  pinMode(GPIO_BUZZER, OUTPUT);
  pinMode(GPIO_SENSOR_HUMEDAD, INPUT);
  pinMode(GPIO_SENSOR_LUZ, INPUT);

  Wire.begin(GPIO_LCD_SDA, GPIO_LCD_SCL);
  lcd.init();
  lcd.backlight();

  mostrar_lcd("Iniciando...");
  estado = ESTADO_INIT;

  queueEvents = xQueueCreate(QUEUE_SIZE, sizeof(stEvento));

  xTaskCreate(vLoopTask, "LoopFSM", STACK_SIZE, NULL, PRIORIDAD_TAREAS, &loopTaskHandler);
  xTaskCreate(vGetNewEventTask, "EventTask", STACK_SIZE, NULL, PRIORIDAD_TAREAS, &loopNewEventHandler);
}

void setup() {
  inicializar();
}

void loop() {
  // No se usa, implementado con con FreeRTOS
}