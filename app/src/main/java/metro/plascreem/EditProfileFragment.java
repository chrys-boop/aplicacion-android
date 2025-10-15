package metro.plascreem;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;
import java.util.Objects;

public class EditProfileFragment extends Fragment {

    private EditText etNombre, etApellidoPaterno, etApellidoMaterno, etExpediente, etTaller, etEnlaceOrigen, etHorario;
    private Button btnGuardar;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    public EditProfileFragment() {
        // Constructor requerido
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseManager = new DatabaseManager(getContext());
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Inicializar vistas
        etNombre = view.findViewById(R.id.et_nombre);
        etApellidoPaterno = view.findViewById(R.id.et_apellido_paterno);
        etApellidoMaterno = view.findViewById(R.id.et_apellido_materno);
        etExpediente = view.findViewById(R.id.et_expediente);
        etTaller = view.findViewById(R.id.et_taller);
        etEnlaceOrigen = view.findViewById(R.id.et_enlace_origen);
        etHorario = view.findViewById(R.id.et_horario);
        btnGuardar = view.findViewById(R.id.btn_guardar_perfil);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Cargar los datos del usuario actual en los EditText
        loadCurrentUserData();

        // Listener del botón Guardar
        btnGuardar.setOnClickListener(v -> saveProfileData());
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (userData != null && isAdded()) {
                        // Descomponer el nombre completo
                        String nombreCompleto = Objects.toString(userData.get("nombreCompleto"), "");
                        String[] nameParts = nombreCompleto.split(" ", 3);
                        String nombre = nameParts.length > 0 ? nameParts[0] : "";
                        String apellidoP = nameParts.length > 1 ? nameParts[1] : "";
                        String apellidoM = nameParts.length > 2 ? nameParts[2] : "";

                        etNombre.setText(nombre);
                        etApellidoPaterno.setText(apellidoP);
                        etApellidoMaterno.setText(apellidoM);

                        etExpediente.setText(Objects.toString(userData.get("numeroExpediente"), ""));
                        etTaller.setText(Objects.toString(userData.get("taller"), ""));
                        etEnlaceOrigen.setText(Objects.toString(userData.get("enlaceOrigen"), ""));
                        etHorario.setText(Objects.toString(userData.get("horario"), ""));
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al cargar datos: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void saveProfileData() {
        // Obtener texto de los campos
        String nombre = etNombre.getText().toString().trim();
        String apellidoPaterno = etApellidoPaterno.getText().toString().trim();
        String apellidoMaterno = etApellidoMaterno.getText().toString().trim();
        String expediente = etExpediente.getText().toString().trim();
        String taller = etTaller.getText().toString().trim();
        String enlaceOrigen = etEnlaceOrigen.getText().toString().trim();
        String horario = etHorario.getText().toString().trim();

        // Validar que los campos no estén vacíos
        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || expediente.isEmpty()) {
            Toast.makeText(getContext(), "Nombre, Apellido y Expediente son obligatorios.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Unir el nombre completo
        String fullName = nombre + " " + apellidoPaterno + " " + apellidoMaterno;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String email = currentUser.getEmail();

            // Llamada al método correcto en DatabaseManager
            databaseManager.updateUserProfile(userId, fullName, email, expediente, taller, enlaceOrigen, horario, new DatabaseManager.DataSaveListener() {
                @Override
                public void onSuccess() {
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(getContext(), "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                        // Regresar al fragmento anterior
                        getParentFragmentManager().popBackStack();
                    }
                }

                @Override
                public void onFailure(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al actualizar: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(), "Error: No se pudo identificar al usuario actual.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
