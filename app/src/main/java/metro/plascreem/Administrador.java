package metro.plascreem;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrador);


        // Suscribir al usuario al topic \"all\" para recibir notificaciones
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
            } else if (itemId == R.id.nav_documentos) {
                selectedFragment = documentsFragment;
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Cargar el fragmento por defecto (Perfil)
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_perfil);
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.admin_fragment_container, fragment);
        transaction.commit();
    }

    /**
     * Suscribe al usuario al topic \"all\" para recibir notificaciones push
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
