/*
 * Este archivo contiene todas las constantes utilizadas en el proyecto
 */
#ifndef CONSTANTES_H
#define CONSTANTES_H

//----------------------------------------------
// Pines del sistema
#define GPIO_RELE              26  // Rele para la válvula
#define GPIO_BOTON             18  // Boton para riego manual
#define GPIO_SENSOR_HUMEDAD    34  // Potenciometro en ADC1_CHANNEL_0
#define GPIO_SENSOR_LUZ        35  // Potenciometro en ADC1_CHANNEL_3
#define GPIO_BUZZER            21  // Buzzer para señalización
#define GPIO_LCD_SDA           23  // SDA del LCD I2C
#define GPIO_LCD_SCL           22  // SCL del LCD I2C
//----------------------------------------------

//----------------------------------------------

#define HUMEDAD_UMBRAL_MIN_PCT   30     // Menor a esto se activa riego automático
#define HUMEDAD_UMBRAL_MAX_PCT   90     // Mayor a esto se detiene riego automático
#define LUZ_UMBRAL             1000   // Valor bajo indica oscuridad
#define MINIMO_HUMEDAD_VALIDA         0   
#define MINIMO_LUZ_VALIDA             0   
//----------------------------------------------

//----------------------------------------------
// Tiempos del sistema
#define TIEMPO_RIEGO_MS        10000  // Duración máxima del riego en milisegundos
//----------------------------------------------
#define MAX_EVENTOS 5
#define FILAS_LCD        2
#define COLUMNAS_LCD     16
#define STACK_SIZE         2048
#define QUEUE_SIZE         10
#define PRIORIDAD_TAREAS      1
#define INTERVALO_TAREAS  20
#define ADC_MAX_VALUE             4095  // Resolución  del ESP32

//----------------------------------------------
#endif // CONSTANTES_H