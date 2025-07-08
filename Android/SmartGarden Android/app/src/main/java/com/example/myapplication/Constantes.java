package com.example.myapplication;

public class Constantes {
    // MQTT Topics
    public static final String TOPIC_SENSOR_HUMEDAD = "sensor/humedad";
    public static final String TOPIC_SENSOR_LUZ = "sensor/luz";
    public static final String TOPIC_ESTADO_RIEGO = "estado/riego";
    public static final String TOPIC_APP_RIEGO = "app/riego";

    // MQTT Values
    public static final String VALOR_RIEGO_ON = "1";
    public static final String VALOR_RIEGO_OFF = "0";

    // MQTT Debug Tags
    public static final String TAG_MQTT = "MQTT";

    // Cálculo de porcentaje
    public static final float SENSOR_MAX_RAW_VALUE = 4086f;

    // Shake detection
    public static final double SHAKE_THRESHOLD = 18.0;
    public static final long SHAKE_COOLDOWN_MS = 1500;
    public static final int VIBRATION_DURATION_MS = 100;

    // UI Textos
    public static final String TEXTO_HUMEDAD = "💧 Humedad: ";
    public static final String TEXTO_LUZ = "☀️ Luz: ";
    public static final String TEXTO_HUMEDAD_ERROR = "💧 Humedad: error";
    public static final String TEXTO_LUZ_ERROR = "☀️ Luz: error";
    public static final String TEXTO_ESTADO_REGANDO = "💦 Estado: Regando";
    public static final String TEXTO_ESTADO_REPOSO = "🌿 Estado: En reposo";
    public static final String TEXTO_BTN_REGAR = "Regar";
    public static final String TEXTO_BTN_DETENER = "Detener Riego";


    // Servidor base
    public static final String SERVER_BASE_URL = "https://setcomnas.ddns.net:4444";

    // Endpoints
    public static final String ENDPOINT_RIEGO_HISTORICO = "/node-red/riego_historico";
    public static final String ENDPOINT_HUMEDAD_HISTORICO = "/node-red/humedad_historico";

    // Etiquetas logs
    public static final String TAG_VOLLEY_RIEGO = "VOLLEY_RIEGO";
    public static final String TAG_VOLLEY_HUMEDAD = "VOLLEY_HUMEDAD";
    public static final String TAG_PARSE_RIEGO = "PARSE_RIEGO";
    public static final String TAG_PARSE_HUMEDAD = "PARSE_HUMEDAD";

    // Gráficos
    public static final float BAR_WIDTH = 0.9f;
    public static final String LABEL_RIEGO = "Veces que se regó";
    public static final String LABEL_HUMEDAD = "Humedad promedio";

    // Broker MQTT
    public static final String MQTT_SERVER_URI = "tcp://setcomnas.ddns.net:1885";
    public static final String MQTT_USERNAME = "usuario";
    public static final String MQTT_PASSWORD = "usuarioadmin";
    // Topics MQTT
    public static final String TOPIC_PROGRAMACION_RIEGO = "programacion/riego";

    // Tipos de recurrencia
    public static final String RECURRENCIA_DIARIA = "Diario";
    public static final String RECURRENCIA_SEMANAL = "Semanal";
    public static final String RECURRENCIA_CADA_X_DIAS = "Cada X días";
    public static final String RECURRENCIA_NO = "no";

    // Logs
    public static final String TAG_PROGRAMACION = "PROGRAMACION";
}
