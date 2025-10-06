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

// Importa la clase de View Binding generada automáticamente
import metro.plascreem.databinding.FragmentAdminProfileBinding;

public class AdminProfileFragment extends Fragment {

    private FragmentAdminProfileBinding binding;

    public AdminProfileFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Usamos View Binding para inflar el layout
        binding = FragmentAdminProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Mostrar datos simulados (reemplazar con datos reales de la BD)
        // Usamos los IDs del XML (tv_nombre, tv_expediente, etc.)
        binding.tvNombre.setText("Juan Pérez Administrativo");
        binding.tvExpediente.setText("Expediente: M-4567");
        binding.tvTaller.setText("Taller: Mantenimiento Mayor");
        binding.tvHorario.setText("Horario: 8:00 a 16:00");

        // 2. Botón Editar Perfil (Llama al Fragmento reutilizable)
        binding.btnEditProfile.setOnClickListener(v -> {
            navigateToEditProfile();
        });

        // 3. Botón Cerrar Sesión
        binding.btnLogout.setOnClickListener(v -> {
            performLogout();
        });

        // 4. Icono de Alertas
        binding.btnAlerts.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Abriendo notificaciones de documentos subidos...", Toast.LENGTH_SHORT).show();
            // TODO: Se puede implementar una notificación o un diálogo aquí
        });
    }

    private void navigateToEditProfile() {
        // Asegúrate que el ID del contenedor es R.id.admin_fragment_container
        // (definido en activity_personal_administrativo.xml)
        int containerId = R.id.admin_fragment_container;

        getParentFragmentManager().beginTransaction()
                .replace(containerId, new EditProfileFragment()) // Reutilizamos tu fragmento existente
                .addToBackStack("Profile") // Permite al usuario volver
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private void performLogout() {
        // Lógica de Cierre de Sesión (Limpia la pila de Activities)
        Toast.makeText(getContext(), "Cerrando Sesión...", Toast.LENGTH_SHORT).show();

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
        // Limpiamos la referencia del binding
        binding = null;
    }
}