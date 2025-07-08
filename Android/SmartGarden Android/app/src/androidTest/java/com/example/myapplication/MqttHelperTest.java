package com.example.myapplication;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class MqttHelperTest {

    @Test
    public void testMqttConnection() throws InterruptedException, MqttException {
        CountDownLatch forever = new CountDownLatch(1); // ⬅️ este es el que se queda esperando

        MqttHelper helper = new MqttHelper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.e("MQTT_TEST", "Conexión perdida", cause);
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        Log.i("MQTT_TEST", "📩 Mensaje recibido: " + topic + " -> " + message.toString());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        Log.d("MQTT_TEST", "Mensaje enviado");
                    }
                }
        );

        // El test se queda vivo para siempre, útil para pruebas manuales
        forever.await(); // nunca termina
    }
}