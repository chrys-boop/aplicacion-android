package metro.plascreem;

import android.app.AlertDialog;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import metro.plascreem.databinding.FragmentCalendarBinding;

public class CalendarFragment extends Fragment
        implements EventosAdapter.EventoActionListener {

    private FragmentCalendarBinding binding;
    private EventosAdapter eventosAdapter;
    private final List<Evento> eventosDelDia = new ArrayList<>();
    private DatabaseManager databaseManager;
    private String userRole;

    public CalendarFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        // --- CORRECCIÓN --- Se usa getContext() en un Fragment.
        databaseManager = new DatabaseManager(getContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventosAdapter = new EventosAdapter(eventosDelDia, this);
        binding.rvEventosDia.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEventosDia.setAdapter(eventosAdapter);

        binding.rvEventosDia.setNestedScrollingEnabled(false);

        binding.calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.tvEventosHeader.setText("Eventos para el: " + dayOfMonth + "/" + (month + 1) + "/" + year);
            loadEventsForDate(selectedDate);
        });

        // Carga inicial de eventos
        Calendar today = Calendar.getInstance();
        String initialDate = String.format("%d-%02d-%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));
        loadEventsForDate(initialDate);

        // Configurar el botón flotante (FAB)
        binding.fabAddEvento.setOnClickListener(v -> handleFabClick());

        // Obtener el rol del usuario actual
        loadUserRole();
    }

    @Override
    public void onEventoClicked(Evento evento) {
        showEventDetailsDialog(evento);
    }


    private void loadUserRole() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && isAdded()) {
            databaseManager.getUserDataMap(currentUser.getUid(), new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (isAdded()) {
                        userRole = (String) userData.get("userType");
                        updateFabVisibility();
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    if (isAdded()) {
                        binding.fabAddEvento.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void updateFabVisibility() {
        if ("Administrador".equals(userRole) || "Personal_Administrativo".equals(userRole)) {
            binding.fabAddEvento.setVisibility(View.VISIBLE);
        } else {
            binding.fabAddEvento.setVisibility(View.GONE);
        }
    }

    private int getFragmentContainerId() {
        if ("Administrador".equals(userRole)) {
            return R.id.admin_fragment_container;
        }
        return R.id.fragment_container;
    }

    private void handleFabClick() {
        if ("Administrador".equals(userRole) || "Personal_Administrativo".equals(userRole)) {
            getParentFragmentManager().beginTransaction()
                    .replace(getFragmentContainerId(), new CreateEventFragment())
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        } else {
            Toast.makeText(getContext(), "No tienes permiso para crear eventos.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadEventsForDate(String date) {
        databaseManager.getEventsForDate(date, new DatabaseManager.EventsListener() {
            @Override
            public void onEventsReceived(List<Evento> events) {
                if (isAdded()) {
                    eventosAdapter.setEventos(events);
                    showEmptyState(events.isEmpty());
                }
            }

            @Override
            public void onCancelled(String message) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error al cargar eventos: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showEventDetailsDialog(Evento evento) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(evento.getTitulo());
        builder.setMessage(evento.getDescripcion());

        // Botón para ejecutar la acción principal del evento (si la tiene)
        if (!Evento.TIPO_SIMPLE.equals(evento.getTipoAccion())) {
            builder.setPositiveButton("Ir a Acción", (dialog, which) -> {
                onActionClick(evento);
            });
        }

        // Botón de cerrar
        builder.setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss());

        // --- BOTÓN DE ELIMINAR (SOLO PARA ADMIN) ---
        if ("Administrador".equals(userRole)) {
            builder.setNeutralButton("Eliminar", (dialog, which) -> {
                // Pedir confirmación antes de eliminar
                new AlertDialog.Builder(getContext())
                        .setTitle("Confirmar Eliminación")
                        .setMessage("¿Estás seguro de que quieres eliminar este evento?")
                        .setPositiveButton("Sí, Eliminar", (confirmDialog, confirmWhich) -> {
                            deleteEvent(evento);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        builder.show();
    }

    private void deleteEvent(Evento evento) {
        databaseManager.deleteEvent(evento.getId(), new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                if(isAdded()) {
                    Toast.makeText(getContext(), "Evento eliminado con éxito", Toast.LENGTH_SHORT).show();
                    // Recargar los eventos para la fecha actual para que desaparezca de la lista
                    loadEventsForDate(evento.getFecha());
                }
            }

            @Override
            public void onFailure(String message) {
                if(isAdded()) {
                    Toast.makeText(getContext(), "Error al eliminar: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void showEmptyState(boolean isEmpty) {
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvEventosDia.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        eventosAdapter.notifyDataSetChanged();

        if (binding.rvEventosDia.getVisibility() == View.VISIBLE) {
            View parent = (View) binding.rvEventosDia.getParent().getParent();
            if (parent != null) {
                parent.requestLayout();
            }
        }
    }

    @Override
    public void onActionClick(Evento evento) {
        Fragment nextFragment = null;

        switch (evento.getTipoAccion()) {
            case Evento.TIPO_DOCUMENTO:
                nextFragment = new UploadDocumentsFragment();
                break;
            case Evento.TIPO_MEDIA:
                nextFragment = new UploadMediaFragment();
                break;
            default:
                return;
        }

        if (nextFragment != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(getFragmentContainerId(), nextFragment) // Usar el container ID correcto
                    .addToBackStack(null)
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
