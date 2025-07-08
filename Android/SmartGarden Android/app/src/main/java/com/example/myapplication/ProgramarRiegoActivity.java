package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Calendar;

/**
 * Pantalla para programar un riego, ya sea único o recurrente.
 * Permite seleccionar fecha, hora, recurrencia y publicar la programación vía MQTT a Node-RED.
 */
public class ProgramarRiegoActivity extends AppCompatActivity {

    // Componentes visuales
    private TextView textFecha, textHora;
    private Switch switchRecurrente;
    private Spinner spinnerFrecuencia;
    private EditText editCadaDias;
    private Button btnConfirmar;

    // Menú lateral
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ImageButton btnMenu;

    // Ayudante para MQTT
    private MqttHelper mqttHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programar_riego);

        // Inicialización de referencias de UI
        textFecha = findViewById(R.id.textFecha);
        textHora = findViewById(R.id.textHora);
        switchRecurrente = findViewById(R.id.switchRecurrente);
        spinnerFrecuencia = findViewById(R.id.spinnerFrecuencia);
        editCadaDias = findViewById(R.id.editCadaDias);
        btnConfirmar = findViewById(R.id.btnConfirmar);

        // Configuración de menú lateral
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btnMenu);

        // Abrir menú al hacer clic en ícono
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Navegación desde el menú lateral
        navView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish(); // Volver a pantalla principal
                return true;
            } else if (item.getItemId() == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardRiegoActivity.class));
                finish();
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });

        // Configurar spinner con opciones de recurrencia
        String[] opciones = {
                Constantes.RECURRENCIA_DIARIA,
                Constantes.RECURRENCIA_SEMANAL,
                Constantes.RECURRENCIA_CADA_X_DIAS
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opciones);
        spinnerFrecuencia.setAdapter(adapter);

        // Mostrar picker de fecha al tocar el campo
        textFecha.setOnClickListener(v -> mostrarDatePicker());

        // Mostrar picker de hora al tocar el campo
        textHora.setOnClickListener(v -> mostrarTimePicker());

        // Mostrar/ocultar opciones según si es recurrente
        switchRecurrente.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerFrecuencia.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            editCadaDias.setVisibility(View.GONE); // Ocultar por defecto el input de "cada X días"
        });

        // Mostrar el campo "cada X días" solo si se selecciona esa opción en el spinner
        spinnerFrecuencia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String seleccion = parent.getItemAtPosition(position).toString();
                editCadaDias.setVisibility(seleccion.equals(Constantes.RECURRENCIA_CADA_X_DIAS) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                editCadaDias.setVisibility(View.GONE);
            }
        });

        // Inicializar MQTT sin callback, ya que solo se usa para publicar
        try {
            mqttHelper = new MqttHelper(getApplicationContext(), null);
        } catch (MqttException e) {
            e.printStackTrace(); // Mostrar error si falla la conexión
        }

        // Enviar programación al presionar confirmar
        btnConfirmar.setOnClickListener(v -> {
            String fecha = textFecha.getText().toString();
            String hora = textHora.getText().toString();
            boolean esRecurrente = switchRecurrente.isChecked();
            String tipoRecurrente = spinnerFrecuencia.getSelectedItem().toString();
            String cadaXdias = editCadaDias.getText().toString();

            // Validar formato de fecha (espera dd/MM/yyyy)
            String[] partesFecha = fecha.split("/");
            if (partesFecha.length != 3) return;

            // Convertir a formato ISO (yyyy-MM-dd)
            String fechaFinal = partesFecha[2] + "-" + partesFecha[1] + "-" + partesFecha[0];

            // Construir mensaje en formato JSON
            String json = "{"
                    + "\"fecha\":\"" + fechaFinal + "\","
                    + "\"hora\":\"" + hora + "\","
                    + "\"recurrente\":\"" + (esRecurrente ? tipoRecurrente.toLowerCase() : Constantes.RECURRENCIA_NO) + "\","
                    + "\"cadaXdias\":\"" + (tipoRecurrente.equals(Constantes.RECURRENCIA_CADA_X_DIAS) ? cadaXdias : "") + "\""
                    + "}";

            // Publicar el mensaje al topic correspondiente
            try {
                mqttHelper.publish(Constantes.TOPIC_PROGRAMACION_RIEGO, String.valueOf(new MqttMessage(json.getBytes())));
            } catch (MqttException e) {
                e.printStackTrace();
            }

            // Cerrar la pantalla luego de confirmar
            finish();
        });
    }

    /**
     * Abre el diálogo para seleccionar una fecha.
     * El resultado se coloca en el TextView correspondiente.
     */
    private void mostrarDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int mes = c.get(Calendar.MONTH);
        int dia = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, mes1, dia1) -> {
                    String fecha = String.format("%02d/%02d/%04d", dia1, mes1 + 1, year1);
                    textFecha.setText(fecha);
                }, year, mes, dia);

        datePickerDialog.show();
    }

    /**
     * Abre el diálogo para seleccionar una hora.
     * El resultado se coloca en el TextView correspondiente.
     */
    private void mostrarTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hora = c.get(Calendar.HOUR_OF_DAY);
        int minutos = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, horaSeleccionada, minutoSeleccionado) -> {
                    String horaFinal = String.format("%02d:%02d", horaSeleccionada, minutoSeleccionado);
                    textHora.setText(horaFinal);
                }, hora, minutos, true);

        timePickerDialog.show();
    }
}