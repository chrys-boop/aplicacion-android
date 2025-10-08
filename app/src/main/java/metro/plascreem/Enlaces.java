package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class Enlaces extends AppCompatActivity {

    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;
    private TextView tvEnlaceName, tvDatosPerfil;

    private final String[] navigationOptions = new String[]{
            "Subir Archivos y Diagramas",
            "Subir Fotos y Videos",
            "Ver Calendario",
            "Actualizar Plantillas e Históricos"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enlaces);

        // Inicializar Firebase y DatabaseManager
        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager();

        // Inicializar vistas
        tvEnlaceName = findViewById(R.id.tv_enlace_name);
        tvDatosPerfil = findViewById(R.id.tv_datos_perfil);
        Spinner spinner = findViewById(R.id.spinner_navigation);
        Button btnEditarPerfil = findViewById(R.id.btn_editar_perfil);
        Button btnCerrarSesion = findViewById(R.id.btn_cerrar_sesion);

        // Cargar datos del enlace
        loadEnlaceData();

        // Configurar el adaptador para el Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                navigationOptions
        );
        spinner.setAdapter(adapter);

        // Manejar el evento de selección del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fragment selectedFragment = null;
                switch (position) {
                    case 0:
                        selectedFragment = new UploadDocumentsFragment();
                        break;
                    case 1:
                        selectedFragment = new UploadMediaFragment();
                        break;
                    case 2:
                        selectedFragment = new CalendarFragment();
                        break;
                    case 3:
                        selectedFragment = new UpdateDataFragment();
                        break;
                }
                if (selectedFragment != null) {
                    replaceFragment(selectedFragment, false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Implementar la acción de Cerrar Sesión
        btnCerrarSesion.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(Enlaces.this, "Sesión Cerrada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Enlaces.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Implementar la acción de Editar Perfil
        btnEditarPerfil.setOnClickListener(v -> {
            replaceFragment(new EditProfileFragment(), true);
        });

        // Cargar el Fragment inicial
        if (savedInstanceState == null) {
            spinner.setSelection(0);
        }
    }

    private void loadEnlaceData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (userData != null) {
                        String nombre = String.valueOf(userData.getOrDefault("nombreCompleto", "Nombre no disponible"));
                        String expediente = String.valueOf(userData.getOrDefault("numeroExpediente", "N/A"));
                        String taller = String.valueOf(userData.getOrDefault("taller", "N/A"));

                        tvEnlaceName.setText(nombre);
                        String profileDetails = "Expediente: " + expediente + "\n" + "Taller: " + taller;
                        tvDatosPerfil.setText(profileDetails);
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    Toast.makeText(Enlaces.this, "Error al cargar datos: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        // Reemplaza el FrameLayout, no el contenedor raíz de la actividad
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

}
