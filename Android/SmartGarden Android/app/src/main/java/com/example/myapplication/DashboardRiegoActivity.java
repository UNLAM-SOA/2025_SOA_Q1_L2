package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.util.ArrayList;

public class DashboardRiegoActivity extends AppCompatActivity {

    // --- Vistas del layout ---
    private BarChart barChart;             // Gráfico de barras para cantidad de riegos por día
    private BarChart barChartHumedad;      // Gráfico de humedad promedio por día
    private DrawerLayout drawerLayout;     // Layout del menú lateral
    private ImageButton btnMenu;           // Botón para abrir el menú
    private NavigationView navView;        // Navegación del menú lateral

    // --- Endpoints para obtener los datos desde Node-RED ---
    private final String urlRiego = Constantes.SERVER_BASE_URL + Constantes.ENDPOINT_RIEGO_HISTORICO;
    private final String urlHumedad = Constantes.SERVER_BASE_URL + Constantes.ENDPOINT_HUMEDAD_HISTORICO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_riego);

        // Inicialización de vistas
        barChart = findViewById(R.id.barChart);
        barChartHumedad = findViewById(R.id.barChartHumedad);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btnMenu);
        navView = findViewById(R.id.nav_view);

        // Abrir menú lateral cuando se toca el botón
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Opciones del menú lateral
        navView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish(); // Volver al MainActivity
                return true;
            } else if (item.getItemId() == R.id.nav_programar_riego) {
                // Ir a la pantalla de programación de riegos
                Intent intent = new Intent(DashboardRiegoActivity.this, ProgramarRiegoActivity.class);
                startActivity(intent);
                finish();
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });

        // Cargar y mostrar datos en los gráficos
        cargarDatosRiego();
        cargarHumedadPromedio();
    }

    /**
     * Carga los datos de cantidad de riegos diarios desde el backend y los muestra en un gráfico de barras.
     */
    private void cargarDatosRiego() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, urlRiego, null,
                response -> {
                    ArrayList<BarEntry> entradas = new ArrayList<>();
                    ArrayList<String> etiquetas = new ArrayList<>();

                    // Parsear el array JSON
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            int cantidad = obj.getInt("cantidad");
                            String fecha = obj.getString("fecha").substring(5, 10); // Mostrar solo mm-dd

                            entradas.add(new BarEntry(i, cantidad));
                            etiquetas.add(fecha);
                        } catch (Exception e) {
                            Log.e(Constantes.TAG_PARSE_RIEGO, "Error al parsear JSON", e);
                        }
                    }

                    // Crear el dataset y configurar el gráfico
                    BarDataSet dataSet = new BarDataSet(entradas, Constantes.LABEL_RIEGO);
                    BarData data = new BarData(dataSet);
                    data.setBarWidth(Constantes.BAR_WIDTH);

                    barChart.setData(data);
                    barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetas));
                    barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    barChart.getXAxis().setGranularity(1f);
                    barChart.getXAxis().setGranularityEnabled(true);
                    barChart.getDescription().setEnabled(false); // Ocultar descripción por defecto
                    barChart.setFitBars(true);
                    barChart.invalidate(); // Redibujar
                },
                error -> Log.e(Constantes.TAG_VOLLEY_RIEGO, "Error al obtener datos", error)
        );

        queue.add(request);
    }

    /**
     * Carga los datos de humedad promedio por día y los muestra en un segundo gráfico.
     */
    private void cargarHumedadPromedio() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, urlHumedad, null,
                response -> {
                    ArrayList<BarEntry> entradas = new ArrayList<>();
                    ArrayList<String> etiquetas = new ArrayList<>();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            float humedad = (float) obj.getDouble("humedad_promedio");
                            String fecha = obj.getString("fecha");

                            entradas.add(new BarEntry(i, humedad));
                            etiquetas.add(fecha);
                        } catch (Exception e) {
                            Log.e(Constantes.TAG_PARSE_HUMEDAD, "Error al parsear JSON de humedad", e);
                        }
                    }

                    BarDataSet dataSet = new BarDataSet(entradas, Constantes.LABEL_HUMEDAD);
                    BarData data = new BarData(dataSet);
                    data.setBarWidth(Constantes.BAR_WIDTH);

                    barChartHumedad.setData(data);
                    barChartHumedad.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetas));
                    barChartHumedad.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    barChartHumedad.getXAxis().setGranularity(1f);
                    barChartHumedad.getXAxis().setGranularityEnabled(true);
                    barChartHumedad.getDescription().setEnabled(false);
                    barChartHumedad.setFitBars(true);
                    barChartHumedad.invalidate();
                },
                error -> Log.e(Constantes.TAG_VOLLEY_HUMEDAD, "Error al obtener humedad", error)
        );

        queue.add(request);
    }
}