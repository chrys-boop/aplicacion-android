package metro.plascreem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PrivacyPolicyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla el layout que creamos para la política de privacidad
        return inflater.inflate(R.layout.fragment_privacy_policy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Busca el TextView donde se mostrará el contenido
        TextView policyContent = view.findViewById(R.id.tv_policy_content);

        // Carga el texto desde los recursos de strings (strings.xml)
        policyContent.setText(R.string.privacy_policy_text);

        // --- MI CÓDIGO AÑADIDO ---
        // 1. Encontrar el botón por su ID
        Button acceptButton = view.findViewById(R.id.btn_accept_policy);

        // 2. Asignar un listener para el evento de clic
        acceptButton.setOnClickListener(v -> {
            // 3. Si el fragmento está adjunto a una actividad, ciérralo.
            if (getActivity() != null) {
                // Esta línea simula el botón de "atrás", cerrando el fragmento actual.
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
}
