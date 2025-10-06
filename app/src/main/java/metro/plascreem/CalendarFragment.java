package metro.plascreem;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Importamos la clase de vinculación generada automáticamente
import metro.plascreem.databinding.FragmentCalendarBinding;

// IMPLEMENTACIÓN CLAVE: El fragmento implementa la interfaz del adaptador para manejar los clics.
public class CalendarFragment extends Fragment
        implements EventosAdapter.EventoActionListener {

    private FragmentCalendarBinding binding;
    private EventosAdapter eventosAdapter;
    private final List<Evento> eventosDelDia = new ArrayList<>();

    public CalendarFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Uso de View Binding
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Configurar RecyclerView: ... (Esto es correcto)
        eventosAdapter = new EventosAdapter(eventosDelDia, this);
        binding.rvEventosDia.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEventosDia.setAdapter(eventosAdapter);


        // **¡NUEVO!** Deshabilitar el scroll interno del RecyclerView
        binding.rvEventosDia.setNestedScrollingEnabled(false);

        // 2. Manejo de la selección del día (esto es correcto)
        binding.calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // ... (tu código para loadEventsForDate(selectedDate);)
            String selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.tvEventosHeader.setText("Eventos para el: " + dayOfMonth + "/" + (month + 1) + "/" + year);
            loadEventsForDate(selectedDate);
        });

        // 3. Carga inicial de eventos (AJUSTE AQUÍ)
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH) + 1;
        int currentYear = today.get(Calendar.YEAR);

        String initialDate;

        // Si hoy no es el día 5 o 10, forzamos la carga del día 5 para que la lista aparezca al inicio
        if (currentDay != 5 && currentDay != 10) {
            // Forzamos la fecha al día 5 del mes actual
            initialDate = String.format("%d-%02d-05", currentYear, currentMonth);
            binding.calendarView.setDate(today.getTimeInMillis()); // Aseguramos que el calendario apunte a hoy
        } else {
            initialDate = String.format("%d-%02d-%02d", currentYear, currentMonth, currentDay);
        }

        // Mostramos la lista inicial (día 5 si no hay eventos hoy)
        loadEventsForDate(initialDate);

        // 4. FAB ... (esto es correcto)
    }

    // --- Lógica de Simulación de Carga de Datos ---
    private void loadEventsForDate(String date) {
        // En un proyecto real, aquí harías una llamada a Supabase/Firebase con la 'date'
        List<Evento> mockEvents = new ArrayList<>();

        // Simulación: Si es el día 5 o 10 del mes, hay eventos
        if (date.endsWith("-05")) {
            // Evento que requiere subir un documento
            mockEvents.add(new Evento("e1", "Subir Informe Trimestral", "Requerido por Dirección.", date, Evento.TIPO_DOCUMENTO));
            // Evento simple, sin botón de acción
            mockEvents.add(new Evento("e2", "Reunión de Personal", "Reunión en Sala Principal.", date, Evento.TIPO_SIMPLE));
        }
        if (date.endsWith("-10")) {
            // Evento que requiere subir fotos o videos
            mockEvents.add(new Evento("e3", "Subir Fotos de Inventario", "Fotos de los productos nuevos.", date, Evento.TIPO_MEDIA));
        }

        eventosAdapter.setEventos(mockEvents);
        showEmptyState(mockEvents.isEmpty());
    }

    private void showEmptyState(boolean isEmpty) {
        // Visibilidad del Empty State
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        // Visibilidad del RecyclerView
        binding.rvEventosDia.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // **¡CRUCIAL!** Forzar a que el adaptador sepa que los datos han cambiado.
        // Esto es especialmente útil después de cambiar la visibilidad de GONE a VISIBLE.
        eventosAdapter.notifyDataSetChanged();

        // Invalida la vista padre para forzar el redibujado completo
        if (binding.rvEventosDia.getVisibility() == View.VISIBLE) {
            View parent = (View) binding.rvEventosDia.getParent().getParent();
            if (parent != null) {
                parent.requestLayout();
            }
        }
    }

    // --- IMPLEMENTACIÓN CLAVE: Manejo del clic del botón en la lista ---
    @Override
    public void onActionClick(Evento evento) {
        Fragment nextFragment = null;

        // Lógica de navegación basada en el tipo de acción del Evento
        switch (evento.getTipoAccion()) {
            case Evento.TIPO_DOCUMENTO:
                nextFragment = new UploadDocumentsFragment();
                break;
                case Evento.TIPO_MEDIA:
                nextFragment = new UploadMediaFragment();
                break;
            default:
                return; // Ignorar otros tipos que no tienen botón
        }

        // Realizar la transacción del Fragmento (navegación)
        if (nextFragment != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment) // Asegúrate que 'fragment_container' es tu ID correcto en MainActivity
                    .addToBackStack(null) // Para que el usuario pueda volver con el botón "Atrás"
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}