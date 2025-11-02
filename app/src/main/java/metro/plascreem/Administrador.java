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
    private final Fragment workerActivityHistoryFragment = new WorkerActivityHistoryFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrador);

        Toolbar toolbar = findViewById(R.id.toolbar_administrador);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab_send_message);
        fab.setOnClickListener(view -> {
            replaceFragment(new SendMessageFragment(), true);
        });

        // Listener para el botón que abre la lista de conversaciones
        FloatingActionButton fabOpenChat = findViewById(R.id.fab_open_chat);
        fabOpenChat.setOnClickListener(view -> {
            replaceFragment(new ConversationsFragment(), true);
        });

        subscribeToNotifications();
        bottomNavigationView = findViewById(R.id.admin_bottom_navigation);
        // Configurar el listener para el BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            // Manejar la selección de elementos del menú
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
                replaceFragment(selectedFragment, false);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_perfil);
        }
    }
    // Crear el menú de opciones
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);
        return true;
    }
    // Manejar la selección de elementos del menú
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_manage_users) {
            replaceFragment(new UserListFragment(), true);
            return true;
        } else if (itemId == R.id.action_change_password) {
            replaceFragment(new ChangePasswordFragment(), true);
            return true;
        } else if (itemId == R.id.action_update_email) {
            replaceFragment(new UpdateEmailFragment(), true);
            return true;
        } else if (itemId == R.id.action_settings) {
            replaceFragment(new SettingsFragment(), true);
            return true;
        } else if (itemId == R.id.action_worker_history) {
            replaceFragment(workerActivityHistoryFragment, true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // Reemplazar el fragmento actual
    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.admin_fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
    // Suscribir al topic "all"
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
