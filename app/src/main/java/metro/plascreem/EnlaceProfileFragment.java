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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enlace_profile, container, false);

        // Inicializar Firebase y DatabaseManager
        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager();

        // Inicializar vistas
        tvEnlaceName = view.findViewById(R.id.tv_enlace_name);
        tvDatosPerfil = view.findViewById(R.id.tv_datos_perfil);
        btnEditarPerfil = view.findViewById(R.id.btn_editar_perfil);
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion);

        // Cargar datos del enlace
        loadEnlaceData();

        // Listener para el botón de editar perfil
        btnEditarPerfil.setOnClickListener(v -> {
            // Reemplaza el contenedor principal de la actividad Enlaces
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
            getActivity().finish();
        });

        return view;
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
                        String taller = String.valueOf(userData.getOrDefault("taller", "N/A"));

                        tvEnlaceName.setText(nombre);
                        String profileDetails = "Expediente: " + expediente + "\n" + "Taller: " + taller;
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
