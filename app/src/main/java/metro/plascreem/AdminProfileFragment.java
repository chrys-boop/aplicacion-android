package metro.plascreem;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Map;

import metro.plascreem.databinding.FragmentAdminProfileBinding;

public class AdminProfileFragment extends Fragment {

    private FragmentAdminProfileBinding binding;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    public AdminProfileFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseManager = new DatabaseManager();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserData();

        // Navegar a la pantalla de Editar Perfil
        binding.btnEditProfile.setOnClickListener(v -> {
            navigateToEditProfile();
        });

        // Cerrar la sesión del usuario
        binding.btnLogout.setOnClickListener(v -> {
            performLogout();
        });

        // Navegar a la pantalla de Seguimiento (Alertas)
        binding.btnAlerts.setOnClickListener(v -> {
            navigateToTracking();
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (userData != null && isAdded()) {
                        binding.tvNombre.setText(String.valueOf(userData.getOrDefault("nombreCompleto", "Nombre no disponible")));
                        binding.tvExpediente.setText("Expediente: " + String.valueOf(userData.getOrDefault("numeroExpediente", "N/A")));
                        binding.tvTaller.setText("Área: " + String.valueOf(userData.getOrDefault("area", "N/A"))); // Campo actualizado
                        binding.tvHorario.setText("Rol: " + String.valueOf(userData.getOrDefault("titularSuplente", "N/A"))); // Campo actualizado
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al cargar los datos: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void navigateToEditProfile() {
        int containerId = R.id.admin_fragment_container;
        getParentFragmentManager().beginTransaction()
                .replace(containerId, new EditProfileFragment())
                .addToBackStack("Profile")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private void navigateToTracking() {
        int containerId = R.id.admin_fragment_container;
        getParentFragmentManager().beginTransaction()
                .replace(containerId, new AdminTrackingFragment()) // Abrimos el fragmento de seguimiento
                .addToBackStack("Profile")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private void performLogout() {
        Toast.makeText(getContext(), "Cerrando Sesión...", Toast.LENGTH_SHORT).show();
        mAuth.signOut(); // Asegurarnos de cerrar sesión en Firebase

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
