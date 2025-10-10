package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;

// Reutilizamos el Fragmento del Calendario
// Reutilizamos el Fragmento de Perfil para la edición

public class Personal_Administrativo extends AppCompatActivity {

    private static final String TAG ="PersonalAdmin";
    // ID del contenedor de fragmentos
    private static final int FRAGMENT_CONTAINER_ID = R.id.admin_fragment_container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_administrativo);

        // Obtener el BottomNavigationView// Suscribir al usuario al topic \"all\" para recibir notificaciones
        subscribeToNotifications();
        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_navigation);

        // Listener para la navegación inferior
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Usamos el ID de cada item definido en menu_admin_navigation.xml
                int itemId = item.getItemId();

                if (itemId == R.id.nav_perfil) {
                    return loadFragment(new AdminProfileFragment(), false);
                } else if (itemId == R.id.nav_archivos) {
                    // Cargar el fragmento para subir/descargar archivos
                    return loadFragment(new AdminFilesFragment(), true);
                } else if (itemId == R.id.nav_calendario) {
                    // Reutilizamos el Fragmento de Calendario
                    return loadFragment(new CalendarFragment(), true);
                } else if (itemId == R.id.nav_documentos) {
                    // Cargar el fragmento para Histórico/Plantillas
                    return loadFragment(new AdminTrackingFragment(), true);
                }
                return false;
            }
        });

        // Cargar el fragmento de Perfil como la vista inicial al abrir la Activity
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_perfil);
        }
    }

    /**
     * Reemplaza el fragmento actual en el contenedor.
     * @param fragment El fragmento a cargar.
     * @param addToBackStack Si se añade a la pila (usar 'true' para navegación secundaria).
     * @return true si la operación fue exitosa.
     */
    private boolean loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(FRAGMENT_CONTAINER_ID, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        } else {
            // Si es una pestaña principal (Perfil/Home), limpiamos la pila
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        transaction.commit();
        return true;
    }
    /**
     * Suscribe al usuario al topic \"all\" para recibir notificaciones push
     */
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