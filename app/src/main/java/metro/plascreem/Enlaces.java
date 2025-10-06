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
import android.widget.Toast;

public class Enlaces extends AppCompatActivity {

    // 1. Definir las opciones del menú
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

        // Inicializar vistas
        Spinner spinner = findViewById(R.id.spinner_navigation);
        Button btnEditarPerfil = findViewById(R.id.btn_editar_perfil);
        Button btnCerrarSesion = findViewById(R.id.btn_cerrar_sesion);

        // 2. Configurar el adaptador para el Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                navigationOptions
        );
        spinner.setAdapter(adapter);

        // 3. Manejar el evento de selección del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        replaceFragment(new UploadDocumentsFragment());
                        break;
                    case 1:
                        replaceFragment(new UploadMediaFragment());
                        break;
                    case 2:
                        replaceFragment(new CalendarFragment());
                        break;
                    case 3:
                        replaceFragment(new UpdateDataFragment());
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 4. Implementar la acción de Cerrar Sesión
        btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 1. Aquí se debe llamar al método de cierre de sesión de Firebase Auth, etc.
                Toast.makeText(Enlaces.this, "Cerrando Sesión...", Toast.LENGTH_SHORT).show();
                Toast.makeText(Enlaces.this, "Sesión Cerrada", Toast.LENGTH_SHORT).show();

                // 2. Navegar a la Activity principal o de inicio de sesión.
                Intent intent = new Intent(Enlaces.this, LoginActivity.class);

                // Flags para asegurar la limpieza de la pila y evitar el regreso al dashboard
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Finaliza esta Activity para que no quede en la pila
            }
        });

        btnEditarPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Enlaces.this, "Editar Perfil", Toast.LENGTH_SHORT).show();
                replaceFragment(new EditProfileFragment());
            }
        });

        // 5. Cargar el Fragment inicial al iniciar la Activity
        if (savedInstanceState == null) {
            spinner.setSelection(0);
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

} // Fin de la clase Enlaces
