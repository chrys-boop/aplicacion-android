package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;

import metro.plascreem.databinding.ActivityTrabajadoresBinding;

public class Trabajadores extends AppCompatActivity {

    private static final String TAG = "Trabajadores";
    private ActivityTrabajadoresBinding binding;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrabajadoresBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- CONFIGURACIÓN DE LA TOOLBAR ---
        setSupportActionBar(binding.toolbarTrabajadores);

        subscribeToNotifications();

        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(getApplicationContext());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(binding.manualsFragmentContainer.getId(), new WorkerManualsFragment())
                    .commit();
        }

        binding.btnEditProfile.setOnClickListener(v -> navigateToEditProfile());

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // --- MÉTODO PARA CREAR EL MENÚ DE OPCIONES ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);
        return true;
    }

    // --- MÉTODO PARA MANEJAR CLICS EN EL MENÚ DE OPCIONES ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_change_password) {
            replaceFragment(new ChangePasswordFragment(), true);
            return true;
        } else if (itemId == R.id.action_update_email) {
            replaceFragment(new UpdateEmailFragment(), true);
            return true;
        } else if (itemId == R.id.action_settings) {
            replaceFragment(new SettingsFragment(), true);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        replaceFragment(new EditProfileFragment(), true);
    }

    // --- MÉTODO AUXILIAR PARA LOS FRAGMENTOS DEL MENÚ ---
    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(binding.mainFragmentContainer.getId(), fragment); // <<--- CORREGIDO
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
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
