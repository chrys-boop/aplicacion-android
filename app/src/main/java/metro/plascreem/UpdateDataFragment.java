package metro.plascreem;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class UpdateDataFragment extends Fragment {

    public UpdateDataFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_data, container, false);

        Button btnActualizar = view.findViewById(R.id.btn_actualizar_plantillas);
        Button btnConfirmar = view.findViewById(R.id.btn_confirmar_historicos);

        btnActualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Iniciando proceso de actualización de plantillas...", Toast.LENGTH_SHORT).show();
                // TODO: Lógica para obtener y actualizar plantillas desde la base de datos
            }
        });

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Abriendo vista para confirmar históricos...", Toast.LENGTH_SHORT).show();
                // TODO: Lógica para navegar a la vista de confirmación de datos históricos
            }
        });

        return view;
    }
}