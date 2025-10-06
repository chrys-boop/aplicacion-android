package metro.plascreem;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import metro.plascreem.databinding.FragmentAdminTrackingBinding;

public class AdminTrackingFragment extends Fragment {

    private FragmentAdminTrackingBinding binding;
    private HistoricoAdapter historicoAdapter;

    public AdminTrackingFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminTrackingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Configurar RecyclerView para el Histórico
        List<HistoricoArchivo> mockData = createMockHistoricoData();
        historicoAdapter = new HistoricoAdapter(mockData);

        binding.rvHistoricoArchivos.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvHistoricoArchivos.setAdapter(historicoAdapter);
        binding.rvHistoricoArchivos.setNestedScrollingEnabled(false); // Necesario por el ScrollView

        // 2. Lógica de Plantillas (Solo simulación de clic)
        binding.btnPlantillaFormato.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Descargando Formato de Incidencias...", Toast.LENGTH_SHORT).show();
            // TODO: Lógica para iniciar la descarga de un archivo fijo
        });

        binding.btnPlantillaDiagrama.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Abriendo Diagrama General...", Toast.LENGTH_SHORT).show();
            // TODO: Lógica para abrir el diagrama (ej. usando un Intent de visualización)
        });
    }

    // --- Simulación de datos del Histórico ---
    private List<HistoricoArchivo> createMockHistoricoData() {
        List<HistoricoArchivo> list = new ArrayList<>();
        list.add(new HistoricoArchivo("Foto de Evidencia Taller A", "jpg", "Enlace: Carlos", "13:30 PM"));
        list.add(new HistoricoArchivo("Reporte Semanal de Tareas", "pdf", "Personal: María", "11:00 AM"));
        list.add(new HistoricoArchivo("Minuta de Reunión de Seguridad", "docx", "Enlace: Pedro", "09:15 AM"));
        list.add(new HistoricoArchivo("Video de Inspección Celdas", "mp4", "Personal: Juan", "Ayer 17:00 PM"));
        list.add(new HistoricoArchivo("Hoja de Mantenimiento Enero", "xlsx", "Administrador: Yo", "Ayer 10:00 AM"));
        return list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
