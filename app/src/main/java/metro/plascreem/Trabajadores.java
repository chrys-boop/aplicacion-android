package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Trabajadores extends AppCompatActivity {

    private TextView tvWorkerName, tvWorkerExpediente;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajadores);

        // Inicializar vistas
        tvWorkerName = findViewById(R.id.tv_worker_name);
        tvWorkerExpediente = findViewById(R.id.tv_worker_expediente);
        btnLogout = findViewById(R.id.btn_logout);

        // Obtener datos del Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String nombre = extras.getString("NOMBRE_COMPLETO", "N/A");
            String expediente = extras.getString("NUMERO_EXPEDIENTE", "N/A");

            tvWorkerName.setText(nombre);
            tvWorkerExpediente.setText("Número de Expediente: " + expediente);
        }

        // Cargar el fragmento de los manuales
        if (savedInstanceState == null) {
            loadWorkerManualsFragment();
        }

        // Configurar botón de salir
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadWorkerManualsFragment() {
        WorkerManualsFragment fragment = new WorkerManualsFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.manuals_fragment_container, fragment);
        fragmentTransaction.commit();
    }
}
