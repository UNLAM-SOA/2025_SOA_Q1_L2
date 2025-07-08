package com.example.myapplication;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    // --- UI ---
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private Button btnRegar;
    private TextView textHumedad, textLuz, textEstado;

    // --- Lógica de negocio ---
    private MqttHelper mqttHelper;
    private boolean estadoRiego = false;

    // --- Sensor de acelerómetro ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0; // Tiempo del último "shake" para evitar repeticiones

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de vistas
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btnMenu);
        btnRegar = findViewById(R.id.btnRegar);
        btnRegar.setBackgroundResource(R.drawable.btn_circle_green); // Estilo circular
        textHumedad = findViewById(R.id.textHumedad);
        textLuz = findViewById(R.id.textLuz);
        textEstado = findViewById(R.id.textEstado);

        // Menú de navegación lateral
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_programar_riego) {
                startActivity(new Intent(MainActivity.this, ProgramarRiegoActivity.class));
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(MainActivity.this, DashboardRiegoActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Estado inicial
        actualizarEstadoUI();

        // Abrir menú lateral
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(androidx.core.view.GravityCompat.START));

        // Inicializar conexión MQTT en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                mqttHelper = new MqttHelper(getApplicationContext(), new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        Log.d(Constantes.TAG_MQTT, "Conexión completada");
                        try {
                            // Suscripciones a los tópicos de sensores y estado
                            mqttHelper.subscribe(Constantes.TOPIC_SENSOR_HUMEDAD);
                            mqttHelper.subscribe(Constantes.TOPIC_SENSOR_LUZ);
                            mqttHelper.subscribe(Constantes.TOPIC_ESTADO_RIEGO);
                        } catch (MqttException e) {
                            Log.e(Constantes.TAG_MQTT, "Error al suscribirse", e);
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.e(Constantes.TAG_MQTT, "Conexión perdida", cause);
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        // Actualización de UI en hilo principal al recibir datos
                        String payload = new String(message.getPayload());
                        runOnUiThread(() -> {
                            switch (topic) {
                                case Constantes.TOPIC_SENSOR_HUMEDAD:
                                    actualizarTextoSensor(textHumedad, payload, Constantes.TEXTO_HUMEDAD, Constantes.TEXTO_HUMEDAD_ERROR);
                                    break;
                                case Constantes.TOPIC_SENSOR_LUZ:
                                    actualizarTextoSensor(textLuz, payload, Constantes.TEXTO_LUZ, Constantes.TEXTO_LUZ_ERROR);
                                    break;
                                case Constantes.TOPIC_ESTADO_RIEGO:
                                    estadoRiego = payload.equals(Constantes.VALOR_RIEGO_ON);
                                    actualizarEstadoUI();
                                    break;
                            }
                        });
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // No se usa en este caso, pero necesario implementar
                    }
                });
            } catch (MqttException e) {
                Log.e(Constantes.TAG_MQTT, "Error al conectar MQTT", e);
            }
        }).start();

        // Evento de click del botón principal (regar)
        btnRegar.setOnClickListener(v -> activarRiego());

        // Efecto visual animado al presionar el botón (rebote)
        btnRegar.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation bounce = AnimationUtils.loadAnimation(MainActivity.this, R.animator.bounce_scale);
                v.startAnimation(bounce);
            }
            return false;
        });

        // Configurar acelerómetro para detectar sacudidas y activar el riego
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SensorEventListener sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Calcular magnitud de la aceleración
                double acceleration = Math.sqrt(x * x + y * y + z * z);
                long now = System.currentTimeMillis();

                // Si se sacude el teléfono con suficiente fuerza, activar riego
                if (acceleration > Constantes.SHAKE_THRESHOLD &&
                        now - lastShakeTime > Constantes.SHAKE_COOLDOWN_MS) {

                    lastShakeTime = now;

                    // Vibración como feedback
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(Constantes.VIBRATION_DURATION_MS);
                    }

                    activarRiego();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // No se usa en este caso, pero necesario implementar
            }
        };

        // Registrar sensor
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * Actualiza el texto de humedad/luz mostrando porcentaje
     */
    private void actualizarTextoSensor(TextView textView, String payload, String label, String fallback) {
        try {
            float raw = Float.parseFloat(payload);
            int porcentaje = Math.round((raw * 100) / Constantes.SENSOR_MAX_RAW_VALUE);
            porcentaje = Math.max(0, Math.min(100, porcentaje)); // Clamp entre 0% y 100%
            textView.setText(label + porcentaje + "%");
        } catch (NumberFormatException e) {
            textView.setText(fallback);
        }
    }

    /**
     * Cambia el texto del botón y el estado visual de la app según si se está regando o no
     */
    private void actualizarEstadoUI() {
        if (estadoRiego) {
            textEstado.setText(Constantes.TEXTO_ESTADO_REGANDO);
            btnRegar.setText(Constantes.TEXTO_BTN_DETENER);
        } else {
            textEstado.setText(Constantes.TEXTO_ESTADO_REPOSO);
            btnRegar.setText(Constantes.TEXTO_BTN_REGAR);
        }
        btnRegar.setBackgroundResource(R.drawable.btn_circle_green);
    }

    /**
     * Cambia el estado del riego y lo publica vía MQTT
     */
    private void activarRiego() {
        estadoRiego = !estadoRiego;
        String nuevoEstado = estadoRiego ? Constantes.VALOR_RIEGO_ON : Constantes.VALOR_RIEGO_OFF;

        new Thread(() -> {
            try {
                mqttHelper.publish(Constantes.TOPIC_APP_RIEGO, nuevoEstado);
            } catch (MqttException e) {
                Log.e(Constantes.TAG_MQTT, "Error publicando estado", e);
            }
        }).start();

        actualizarEstadoUI();
    }
}