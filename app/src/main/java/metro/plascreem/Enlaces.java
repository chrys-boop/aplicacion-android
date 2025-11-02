package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

// Imports para el botón flotante y el fragmento de mensaje
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import metro.plascreem.SendMessageFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

public class Enlaces extends AppCompatActivity {

    private static final String TAG = "Enlaces";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enlaces);

        // --- CONFIGURACIÓN DE LA TOOLBAR ---
        Toolbar toolbar = findViewById(R.id.toolbar_enlaces);
        setSupportActionBar(toolbar);

        // --- LÓGICA DEL BOTÓN FLOTANTE ---
        FloatingActionButton fab = findViewById(R.id.fab_send_message);
        fab.setOnClickListener(view -> {
            // Carga el fragmento para enviar mensajes dirigidos
            replaceFragment(new SendMessageFragment(), true);
        });

        // Listener para el botón que abre la lista de conversaciones
        FloatingActionButton fabOpenChat = findViewById(R.id.fab_open_chat);
        fabOpenChat.setOnClickListener(view -> {
            replaceFragment(new ConversationsFragment(), true);
        });

        // Suscribir al usuario al topic "all" para recibir notificaciones
        subscribeToNotifications();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setItemIconTintList(null);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Cargar el fragmento de perfil por defecto
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new EnlaceProfileFragment()).commit();
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
        }
        return super.onOptionsItemSelected(item);
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_profile) {
                selectedFragment = new EnlaceProfileFragment();
            } else if (itemId == R.id.navigation_upload_docs) {
                selectedFragment = new UploadDocumentsFragment();
            } else if (itemId == R.id.navigation_upload_media) {
                selectedFragment = new UploadMediaFragment();
            } else if (itemId == R.id.navigation_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.navigation_update_data) {
                selectedFragment = new UpdateDataFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            }
            return true;
        }
    };

    public void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    private void subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Enlace suscrito exitosamente al topic 'all'");
                    } else {
                        Log.e(TAG, "Error al suscribir al topic 'all': " + task.getException());
                    }
                });
    }
}
