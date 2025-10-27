package metro.plascreem;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;

// Imports para el botón flotante y el fragmento de mensaje
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import metro.plascreem.SendMessageFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

public class Administrador extends AppCompatActivity {

    private static final String TAG = "Administrador";

    private BottomNavigationView bottomNavigationView;

    // Definimos los fragmentos para cada sección
    private final Fragment profileFragment = new AdminProfileFragment();
    private final Fragment filesFragment = new AdminFilesFragment();
    private final Fragment calendarFragment = new CalendarFragment();
    private final Fragment documentsFragment = new UploadDocumentsFragment();
    private final Fragment eventListFragment = new EventListFragment();
    // Añadimos el nuevo fragmento de historial de actividad
    private final Fragment workerActivityHistoryFragment = new WorkerActivityHistoryFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrador);

        // --- CONFIGURACIÓN DE LA TOOLBAR ---
        Toolbar toolbar = findViewById(R.id.toolbar_administrador);
        setSupportActionBar(toolbar);

        // --- LÓGICA DEL BOTÓN FLOTANTE ---
        FloatingActionButton fab = findViewById(R.id.fab_send_message);
        fab.setOnClickListener(view -> {
            // Carga el fragmento para enviar mensajes dirigidos
            replaceFragment(new SendMessageFragment(), true);
        });

        // Suscribir al usuario al topic "all" para recibir notificaciones
        subscribeToNotifications();
        bottomNavigationView = findViewById(R.id.admin_bottom_navigation);

        // Listener para la navegación
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_perfil) {
                selectedFragment = profileFragment;
            } else if (itemId == R.id.nav_archivos) {
                selectedFragment = filesFragment;
            } else if (itemId == R.id.nav_calendario) {
                selectedFragment = calendarFragment;
            } else if (itemId == R.id.nav_event_history) {
                selectedFragment = eventListFragment;
            } else if (itemId == R.id.nav_documentos) {
                selectedFragment = documentsFragment;
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment, false); // No añadir a la pila de retroceso
                return true;
            }
            return false;
        });

        // Cargar el fragmento por defecto (Perfil)
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_perfil);
        }
    }

    // --- MÉTODO PARA CREAR EL MENÚ DE OPCIONES EN LA TOOLBAR ---
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

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.admin_fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null); // Permite volver al fragmento anterior
        }
        transaction.commit();
    }

    /**
     * Suscribe al usuario al topic "all" para recibir notificaciones push
     */
    private void subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Administrador suscrito exitosamente al topic 'all'");
                    } else {
                        Log.e(TAG, "Error al suscribir al topic 'all': " + task.getException());
                    }
                });
    }
}
