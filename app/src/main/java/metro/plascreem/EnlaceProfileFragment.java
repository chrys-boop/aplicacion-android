
package metro.plascreem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class EnlaceProfileFragment extends Fragment {

    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;
    private TextView tvEnlaceName, tvDatosPerfil;
    private Button btnEditarPerfil, btnCerrarSesion;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializar Firebase y DatabaseManager
        mAuth = FirebaseAuth.getInstance();
        // --- CORRECCIÓN: Inicializar DatabaseManager con el contexto ---
        databaseManager = new DatabaseManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enlace_profile, container, false);

        // Inicializar vistas
        tvEnlaceName = view.findViewById(R.id.tv_enlace_name);
        tvDatosPerfil = view.findViewById(R.id.tv_datos_perfil);
        btnEditarPerfil = view.findViewById(R.id.btn_editar_perfil);
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion);

        // Listener para el botón de editar perfil
        btnEditarPerfil.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Listener para el botón de cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Sesión Cerrada", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar los datos del perfil cuando el fragmento se vuelve a mostrar
        loadEnlaceData();
    }

    private void loadEnlaceData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && isAdded()) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (userData != null && isAdded()) {
                        String nombre = String.valueOf(userData.getOrDefault("nombreCompleto", "Nombre no disponible"));
                        String expediente = String.valueOf(userData.getOrDefault("numeroExpediente", "N/A"));

                        // --- CORRECCIÓN: Usar las claves correctas ("area" y "titular") ---
                        String area = String.valueOf(userData.getOrDefault("area", "N/A"));
                        String titular = String.valueOf(userData.getOrDefault("titular", "N/A"));

                        tvEnlaceName.setText(nombre);
                        String profileDetails = "Expediente: " + expediente + "\n" +
                                "Área: " + area + "\n" +
                                "Rol: " + titular;
                        tvDatosPerfil.setText(profileDetails);
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
}
