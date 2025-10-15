package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;

// Importar la clase de View Binding generada
import metro.plascreem.databinding.ActivityTrabajadoresBinding;

public class Trabajadores extends AppCompatActivity {

    private static final String TAG = "Trabajadores";

    // Declarar el objeto de View Binding
    private ActivityTrabajadoresBinding binding;

    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflar el layout usando View Binding
        binding = ActivityTrabajadoresBinding.inflate(getLayoutInflater());
        // Establecer la vista raíz del binding como el contenido de la actividad
        setContentView(binding.getRoot());

        subscribeToNotifications();

        // --- INICIALIZACIÓN ---
        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(getApplicationContext());

        // Cargar el fragmento de los manuales por defecto
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(binding.manualsFragmentContainer.getId(), new WorkerManualsFragment())
                    .commit();
        }

        // --- CORRECCIÓN: CONFIGURACIÓN DE LISTENERS USANDO BINDING DIRECTAMENTE ---
        binding.btnEditProfile.setOnClickListener(v -> {
            navigateToEditProfile();
        });

        binding.btnLogout.setOnClickListener(v -> {
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
                        // --- CORRECCIÓN: Usar binding para acceder a los TextViews directamente ---
                        binding.tvWorkerName.setText(String.valueOf(userData.getOrDefault("nombreCompleto", "Nombre no disponible")));

                        String expediente = String.valueOf(userData.getOrDefault("numeroExpediente", "N/A"));
                        String area = String.valueOf(userData.getOrDefault("area", "N/A"));
                        String titular = String.valueOf(userData.getOrDefault("titular", "N/A"));

                        String workerDetails = "Expediente: " + expediente + "\n" +
                                "Área: " + area + "\n" +
                                "Rol: " + titular;
                        binding.tvWorkerExpediente.setText(workerDetails);
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
        getSupportFragmentManager().beginTransaction()
                .replace(binding.trabajadoresContainer.getId(), new EditProfileFragment())
                .addToBackStack(null)
                .commit();
    }

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
