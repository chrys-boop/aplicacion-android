package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class Enlaces extends AppCompatActivity {

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

        // Cargar el fragmento de perfil por defecto
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.profile_container, new EnlaceProfileFragment())
                    .commit();
        }

        Spinner spinner = findViewById(R.id.spinner_navigation);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                navigationOptions
        );
        spinner.setAdapter(adapter);

        // Manejar la selección del Spinner
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
                    // Reemplaza el contenedor de contenido, no el de perfil
                    replaceFragment(selectedFragment, false, R.id.fragment_container);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Cargar el primer fragmento del Spinner por defecto
        if (savedInstanceState == null) {
            spinner.setSelection(0);
        }
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack, int containerId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(containerId, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
