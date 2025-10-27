package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;

// Importamos las clases necesarias
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;
import metro.plascreem.SendMessageFragment; // <-- El fragmento correcto

public class Personal_Administrativo extends AppCompatActivity {

    private static final String TAG ="PersonalAdmin";
    private static final int FRAGMENT_CONTAINER_ID = R.id.admin_fragment_container;
    private final Fragment workerActivityHistoryFragment = new WorkerActivityHistoryFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_administrativo);

        // --- CONFIGURACIÓN DE LA TOOLBAR ---
        Toolbar toolbar = findViewById(R.id.toolbar_personal_administrativo);
        setSupportActionBar(toolbar);

        // --- LÓGICA DEL BOTÓN FLOTANTE ---
        FloatingActionButton fab = findViewById(R.id.fab_send_message);
        fab.setOnClickListener(view -> {
            // Carga el fragmento para enviar mensajes dirigidos, como se especificó
            replaceFragment(new SendMessageFragment(), true);
        });

        // Suscribir al usuario al topic \"all\" para recibir notificaciones
        subscribeToNotifications();

        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_navigation);
        bottomNav.setItemIconTintList(null);

        // Listener para la navegación inferior
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_perfil) {
                return loadFragment(new AdminProfileFragment(), false);
            } else if (itemId == R.id.nav_archivos) {
                return loadFragment(new AdminFilesFragment(), true);
            } else if (itemId == R.id.nav_calendario) {
                return loadFragment(new CalendarFragment(), true);
            } else if (itemId == R.id.nav_documentos) {
                return loadFragment(new AdminTrackingFragment(), true);
            }
            return false;
        });

        // Cargar el fragmento de Perfil como la vista inicial
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_perfil);
        }
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
        } else if (itemId == R.id.action_worker_history) { // NUEVA CONDICIÓN
            replaceFragment(workerActivityHistoryFragment, true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(FRAGMENT_CONTAINER_ID, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        } else {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        transaction.commit();
        return true;
    }

    // --- MÉTODO AUXILIAR PARA LOS FRAGMENTOS DEL MENÚ DE OPCIONES ---
    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(FRAGMENT_CONTAINER_ID, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    private void subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Personal Administrativo suscrito exitosamente al topic 'all'");
                    } else {
                        Log.e(TAG, "Error al suscribir al topic 'all': " + task.getException());
                    }
                });
    }
}

