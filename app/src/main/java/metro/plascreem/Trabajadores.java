package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;


public class Trabajadores extends AppCompatActivity {

    private static final String TAG = "Trabajadores";

    private TextView tvWorkerName, tvWorkerDetails;
    private Button btnLogout, btnEditProfile;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajadores);

        // Suscribir al usuario al topic \"all\" para recibir notificaciones
        subscribeToNotifications();
        // Inicializar Firebase y DatabaseManager
        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(this);

        // Inicializar vistas
        tvWorkerName = findViewById(R.id.tv_worker_name);
        tvWorkerDetails = findViewById(R.id.tv_worker_expediente); // Reutilizamos este TextView para más detalles
        btnLogout = findViewById(R.id.btn_logout);
        btnEditProfile = findViewById(R.id.btn_edit_profile);

        // Cargar datos del trabajador
        loadWorkerData();

        // Cargar el fragmento de los manuales por defecto
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.manuals_fragment_container, new WorkerManualsFragment())
                    .commit();
        }

        // Configurar botón de editar perfil
        btnEditProfile.setOnClickListener(v -> {
            navigateToEditProfile();
        });

        // Configurar botón de salir
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar los datos cuando se vuelve a esta actividad
        loadWorkerData();
    }

    private void loadWorkerData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (userData != null) {
                        tvWorkerName.setText(String.valueOf(userData.getOrDefault("nombreCompleto", "Nombre no disponible")));

                        String expediente = String.valueOf(userData.getOrDefault("numeroExpediente", "N/A"));
                        String area = String.valueOf(userData.getOrDefault("area", "N/A"));
                        String titularSuplente = String.valueOf(userData.getOrDefault("titularSuplente", "N/A"));

                        String workerDetails = "Expediente: " + expediente + "\n" +
                                "Área: " + area + "\n" +
                                "Rol: " + titularSuplente;
                        tvWorkerDetails.setText(workerDetails);
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    Toast.makeText(Trabajadores.this, "Error al cargar los datos: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void navigateToEditProfile() {
        // Reemplazar todo el contenido de la Activity con el fragmento de edición
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.trabajadores_container, new EditProfileFragment()) // Usa el ID del contenedor raíz
                .addToBackStack(null) // Permite volver a la vista anterior con el botón de retroceso
                .commit();
    }

    /**
     * Suscribe al usuario al topic \"all\" para recibir notificaciones push
     */
    private void subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Trabajador suscrito exitosamente al topic 'all'");
                    } else {
                        Log.e(TAG, "Error al suscribir al topic 'all': " + task.getException());
                    }
                });
    }
}
