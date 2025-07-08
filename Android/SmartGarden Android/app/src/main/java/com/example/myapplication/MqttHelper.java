package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Helper para gestionar la conexión MQTT usando la librería Eclipse Paho en Android.
 * Encapsula la lógica de conexión, suscripción y publicación de mensajes.
 */
public class MqttHelper {

    private final MqttAndroidClient mqttAndroidClient;
    private final String clientId = MqttClient.generateClientId(); // ID único por cliente

    /**
     * Constructor que establece y conecta el cliente MQTT con las opciones definidas.
     *
     * @param context  contexto de la aplicación
     * @param callback callback para manejar eventos de conexión, mensajes, errores, etc.
     * @throws MqttException si falla al crear o conectar el cliente
     */
    public MqttHelper(Context context, MqttCallback callback) throws MqttException {
        // Forzar el uso de HandlerPingSender para mantener viva la conexión en Android
        System.setProperty("org.eclipse.paho.client.ping.sender",
                "org.eclipse.paho.android.service.handler.HandlerPingSender");

        // Se asegura de usar el ApplicationContext para evitar fugas de memoria
        Context appContext = context.getApplicationContext();

        // Crear instancia del cliente MQTT con URI del broker y clientId aleatorio
        mqttAndroidClient = new MqttAndroidClient(
                appContext,
                Constantes.MQTT_SERVER_URI,
                clientId
        );

        mqttAndroidClient.setCallback(callback); // Se asigna el callback recibido

        // Configuración de conexión
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true); // No guarda estado anterior
        options.setAutomaticReconnect(true); // Reintenta reconectar automáticamente
        options.setUserName(Constantes.MQTT_USERNAME); // Usuario si el broker lo requiere
        options.setPassword(Constantes.MQTT_PASSWORD.toCharArray()); // Contraseña

        // Intentar la conexión
        mqttAndroidClient.connect(options, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(Constantes.TAG_MQTT, "Conectado al broker MQTT");

                // Al conectar, suscribirse automáticamente a los topics necesarios
                try {
                    subscribe(Constantes.TOPIC_SENSOR_HUMEDAD);
                    subscribe(Constantes.TOPIC_SENSOR_LUZ);
                } catch (MqttException e) {
                    Log.e(Constantes.TAG_MQTT, "Error al suscribirse luego de conectar", e);
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(Constantes.TAG_MQTT, "Error al conectar al broker: " + exception.getMessage());
                exception.printStackTrace(); // Imprime traza en Logcat para depurar
            }
        });
    }

    /**
     * Suscribe al cliente al topic indicado con QoS 1.
     *
     * @param topic el nombre del topic
     * @throws MqttException si ocurre un error durante la suscripción
     */
    void subscribe(String topic) throws MqttException {
        mqttAndroidClient.subscribe(topic, 1, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(Constantes.TAG_MQTT, "Suscripto al topic: " + topic);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(Constantes.TAG_MQTT, "Falló la suscripción al topic: " + topic, exception);
            }
        });
    }

    /**
     * Publica un mensaje en el topic indicado.
     *
     * @param topic   nombre del topic
     * @param payload contenido del mensaje en texto plano
     * @throws MqttException si ocurre un error al publicar
     */
    public void publish(String topic, String payload) throws MqttException {
        mqttAndroidClient.publish(topic, payload.getBytes(), 1, false);
    }
}