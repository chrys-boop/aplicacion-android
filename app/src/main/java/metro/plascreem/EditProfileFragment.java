package metro.plascreem;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileFragment extends Fragment {

    private EditText etNombre, etApellidoPaterno, etApellidoMaterno, etExpediente, etTaller, etEnlaceOrigen, etHorario;
    private Button btnGuardar;
    private DatabaseManager databaseManager;

    public EditProfileFragment() {
        // Constructor requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        databaseManager = new DatabaseManager();

        etNombre = view.findViewById(R.id.et_nombre);
        etApellidoPaterno = view.findViewById(R.id.et_apellido_paterno);
        etApellidoMaterno = view.findViewById(R.id.et_apellido_materno);
        etExpediente = view.findViewById(R.id.et_expediente);
        etTaller = view.findViewById(R.id.et_taller);
        etEnlaceOrigen = view.findViewById(R.id.et_enlace_origen);
        etHorario = view.findViewById(R.id.et_horario);
        btnGuardar = view.findViewById(R.id.btn_guardar_perfil);

        // Listener del botón Guardar
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });

        // Retornar la vista
        return view;
    }

    private void saveProfileData() {
        String nombre = etNombre.getText().toString().trim();
        String apellidoPaterno = etApellidoPaterno.getText().toString().trim();
        String apellidoMaterno = etApellidoMaterno.getText().toString().trim();
        String expediente = etExpediente.getText().toString().trim();
        String taller = etTaller.getText().toString().trim();
        String enlaceOrigen = etEnlaceOrigen.getText().toString().trim();
        String horario = etHorario.getText().toString().trim();

        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || apellidoMaterno.isEmpty() || expediente.isEmpty() || taller.isEmpty() || enlaceOrigen.isEmpty() || horario.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, rellene todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = nombre + " " + apellidoPaterno + " " + apellidoMaterno;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.updateUserProfile(userId, fullName, currentUser.getEmail(), new DatabaseManager.DataSaveListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                    // Opcional: navegar a otro fragmento
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(getContext(), "Error al actualizar el perfil: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No se ha podido obtener el usuario actual.", Toast.LENGTH_SHORT).show();
        }
    }
}
