package metro.plascreem;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class EditProfileFragment extends Fragment {

    public EditProfileFragment() {
        // Constructor requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        Button btnGuardar = view.findViewById(R.id.btn_guardar_perfil);

        // Listener del botón Guardar
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 1. Leer los datos de los EditTexts
                // TODO: 2. Validar que no estén vacíos
                // TODO: 3. Guardar los datos en la base de datos (Firestore/Realtime DB)
                Toast.makeText(getContext(), "Datos del perfil listos para guardar...", Toast.LENGTH_SHORT).show();
            }
        });

        // Retornar la vista
        return view;
    }
}
