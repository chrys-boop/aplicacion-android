
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

    // Campos existentes
    private EditText etNombre, etApellidoPaterno, etApellidoMaterno, etExpediente, etTaller, etEnlaceOrigen, etHorario;
    // Nuevos campos
    private EditText etArea, etTitular;
    private Button btnGuardar;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    public EditProfileFragment() {
        // Constructor requerido
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseManager = new DatabaseManager(requireContext()); // Usar requireContext() para asegurar que no sea nulo
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Inicializar vistas existentes
        etNombre = view.findViewById(R.id.et_nombre);
        etApellidoPaterno = view.findViewById(R.id.et_apellido_paterno);
        etApellidoMaterno = view.findViewById(R.id.et_apellido_materno);
        etExpediente = view.findViewById(R.id.et_expediente);
        etTaller = view.findViewById(R.id.et_taller);
        etEnlaceOrigen = view.findViewById(R.id.et_enlace_origen);
        etHorario = view.findViewById(R.id.et_horario);
        btnGuardar = view.findViewById(R.id.btn_guardar_perfil);

        // --- INICIALIZAR NUEVAS VISTAS ---
        // Asumiendo que estos IDs existen en tu fragment_edit_profile.xml
        etArea = view.findViewById(R.id.et_area);
        etTitular = view.findViewById(R.id.et_titular);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCurrentUserData();
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

                        // --- CARGAR DATOS EN NUEVOS CAMPOS ---
                        etArea.setText(Objects.toString(userData.get("area"), ""));
                        etTitular.setText(Objects.toString(userData.get("titular"), ""));
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
        // Obtener texto de los campos existentes
        String nombre = etNombre.getText().toString().trim();
        String apellidoPaterno = etApellidoPaterno.getText().toString().trim();
        String apellidoMaterno = etApellidoMaterno.getText().toString().trim();
        String expediente = etExpediente.getText().toString().trim();
        String taller = etTaller.getText().toString().trim();
        String enlaceOrigen = etEnlaceOrigen.getText().toString().trim();
        String horario = etHorario.getText().toString().trim();

        // --- OBTENER TEXTO DE NUEVOS CAMPOS ---
        String area = etArea.getText().toString().trim();
        String titular = etTitular.getText().toString().trim();

        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || expediente.isEmpty()) {
            Toast.makeText(getContext(), "Nombre, Apellido y Expediente son obligatorios.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = (nombre + " " + apellidoPaterno + " " + apellidoMaterno).trim();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String email = currentUser.getEmail();

            // --- LLAMAR AL MÉTODO ACTUALIZADO EN DATabasemanager ---
            databaseManager.updateUserProfile(userId, fullName, email, expediente, taller, enlaceOrigen, horario, area, titular, new DatabaseManager.DataSaveListener() {
                @Override
                public void onSuccess() {
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(getContext(), "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack(); // Regresar
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
