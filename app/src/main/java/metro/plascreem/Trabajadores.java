package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// Se agregaron estas importaciones para visualizar y descargar PDFs
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Environment;

import java.util.ArrayList;
import java.util.List;

public class Trabajadores extends AppCompatActivity {

    private TextView tvWorkerName, tvWorkerExpediente;
    private Button btnLogout;
    private RecyclerView rvManuals;
    private ManualAdapter manualAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajadores);

        // Inicializar vistas
        tvWorkerName = findViewById(R.id.tv_worker_name);
        tvWorkerExpediente = findViewById(R.id.tv_worker_expediente);
        btnLogout = findViewById(R.id.btn_logout);
        rvManuals = findViewById(R.id.rv_manuals);

        // -------------------------------------------------------------
        // MODIFICACIÓN CLAVE: Obtener datos del Intent
        // -------------------------------------------------------------
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String nombre = extras.getString("NOMBRE_COMPLETO", "N/A");
            String expediente = extras.getString("NUMERO_EXPEDIENTE", "N/A");

            tvWorkerName.setText(nombre);
            tvWorkerExpediente.setText("Número de Expediente: " + expediente);
        }

        // Configurar el RecyclerView
        rvManuals.setLayoutManager(new LinearLayoutManager(this));

        // Cargar la lista de manuales (simulado por ahora)
        List<Manual> manuals = new ArrayList<>();
        manuals.add(new Manual("Manual de Mantenimiento de Trenes", "https://ejemplo.com/manual_trenes.pdf"));
        manuals.add(new Manual("Procedimiento de Seguridad en Vías", "https://ejemplo.com/seguridad_vias.pdf"));
        manuals.add(new Manual("Guía de Operación de Equipo", "https://ejemplo.com/guia_equipo.pdf"));
        // Se usa `Trabajadores.this` en el Toast para referenciar la actividad actual
        manualAdapter = new ManualAdapter(manuals, new ManualAdapter.OnManualClickListener() {
            @Override
            public void onManualClick(Manual manual, String action) {
                if (action.equals("visualizar")) {
                    // Lógica para abrir el PDF
                    openPdf(manual.getUrl());
                } else if (action.equals("descargar")) {
                    // Lógica para descargar el PDF
                    downloadPdf(manual.getUrl(), manual.getTitle());
                }
            }
        });
        rvManuals.setAdapter(manualAdapter);

        // Configurar botón de salir
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Métodos para visualizar y descargar
    private void openPdf(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se encontró una aplicación para abrir PDFs.", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadPdf(String url, String title) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title + ".pdf");

        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(this, "Descarga iniciada...", Toast.LENGTH_SHORT).show();
        }
    }

    // Clase de datos para el manual
    public static class Manual {
        String title;
        String url;

        public Manual(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }
}