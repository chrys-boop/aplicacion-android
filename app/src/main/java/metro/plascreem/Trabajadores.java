package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Objects;

import metro.plascreem.databinding.ActivityTrabajadoresBinding;

public class Trabajadores extends AppCompatActivity {

    private static final String TAG = "Trabajadores";
    private ActivityTrabajadoresBinding binding;
    private DatabaseManager databaseManager;
    private ExcelManager excelManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrabajadoresBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarTrabajadores);
        subscribeToNotifications();

        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(getApplicationContext());
        excelManager = new ExcelManager(getApplicationContext());

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);
        return true;
    }

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
                public void onDataReceived(Map<String, Object> firebaseData) {
                    if (firebaseData != null) {
                        binding.tvWorkerName.setText(Objects.toString(firebaseData.get("nombreCompleto"), "NOMBRE NO DISPONIBLE").toUpperCase());

                        String expediente = Objects.toString(firebaseData.get("numeroExpediente"), "");
                        Map<String, Object> excelData = excelManager.findUserByExpediente(expediente);

                        Map<String, Object> dataToUse = (excelData != null) ? excelData : firebaseData;

                        String categoria = Objects.toString(dataToUse.get("categoria"), "N/A");
                        if (categoria.isEmpty()) categoria = "N/A";

                        String fechaIngreso = Objects.toString(dataToUse.get("fechaIngreso"), "N/A");
                        if (fechaIngreso.isEmpty()) fechaIngreso = "N/A";

                        String horarioEntrada = Objects.toString(dataToUse.get("horarioEntrada"), "N/A");
                        if (horarioEntrada.isEmpty()) horarioEntrada = "N/A";

                        String horarioSalida = Objects.toString(dataToUse.get("horarioSalida"), "N/A");
                        if (horarioSalida.isEmpty()) horarioSalida = "N/A";

                        String displayExpediente = Objects.toString(firebaseData.get("numeroExpediente"), "N/A");

                        String workerDetails = "EXPEDIENTE: " + displayExpediente + "\n" +
                                "CATEGORÍA: " + categoria + "\n" +
                                "FECHA DE INGRESO: " + fechaIngreso + "\n" +
                                "HORARIO: " + horarioEntrada + " - " + horarioSalida;
                        binding.tvWorkerExpediente.setText(workerDetails.toUpperCase());
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
        replaceFragment(new EditWorkerProfileFragment(), true);
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(binding.mainFragmentContainer.getId(), fragment);
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


