/*
 * Este archivo contiene todas las constantes utilizadas en el proyecto
 */
#ifndef CONSTANTES_H
#define CONSTANTES_H

//----------------------------------------------
// Pines del sistema
#define GPIO_RELE              26  // Rele para la válvula
#define GPIO_BOTON             18  // Botón para riego manual
#define GPIO_SENSOR_HUMEDAD    34  // Potenciómetro en ADC1_CHANNEL_0
#define GPIO_SENSOR_LUZ        35  // Potenciómetro en ADC1_CHANNEL_3
#define GPIO_BUZZER            21  // Buzzer para señalización
#define GPIO_LCD_SDA           23  // SDA del LCD I2C
#define GPIO_LCD_SCL           22  // SCL del LCD I2C
//----------------------------------------------

//----------------------------------------------
// Umbrales y validaciones
#define HUMEDAD_UMBRAL_MIN_PCT     30   // Menor a esto se activa riego automático
#define HUMEDAD_UMBRAL_MAX_PCT     45   // Mayor a esto se detiene riego automático
#define LUZ_UMBRAL                400   // Valor bajo indica oscuridad
#define MINIMO_HUMEDAD_VALIDA       0
#define MINIMO_LUZ_VALIDA           0
#define UMBRAL_CAMBIO_MINIMO       50   // Mínima variación esperada del sensor de humedad
#define MAX_INTENTOS_RIEGO          2   // Máximos intentos si no varía el sensor
//----------------------------------------------

//----------------------------------------------
// Tiempos del sistema
#define TIEMPO_RIEGO_MS           10000 // Duración máxima del riego en milisegundos
#define INTERVALO_TAREAS            20
#define INTERVALO_ENVIO_UBIDOTS   400
//----------------------------------------------

//----------------------------------------------
// LCD y tareas
#define MAX_EVENTOS         5
#define FILAS_LCD           2
#define COLUMNAS_LCD       16
#define STACK_SIZE       4096
#define QUEUE_SIZE         10
#define PRIORIDAD_TAREAS    1
#define ADC_MAX_VALUE    4095
//----------------------------------------------

//----------------------------------------------
// WiFi y MQTT (Mosquitto)
#define WIFISSID "Ernesto"
#define PASSWORD "Bienvenido123"
/*#define WIFISSID "SO Avanzados"
#define PASSWORD "flatronE2340"*/
#define MQTT_BROKER "setcomnas.ddns.net"  // IP del broker Mosquitto
#define MQTT_USER "usuario"
#define MQTT_PASSWORD "usuarioadmin"

#define TOPIC_HUMEDAD "sensor/humedad"
#define TOPIC_LUZ     "sensor/luz"
#define TOPIC_RIEGO_ESTADO   "estado/riego"
#define TOPIC_RIEGO_CONTROL   "control/riego"
//----------------------------------------------

#endif // CONSTANTES_H
