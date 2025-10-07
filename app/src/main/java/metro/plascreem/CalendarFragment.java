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
        databaseManager = new DatabaseManager();
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

    private void loadUserRole() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            databaseManager.getUserDataMap(currentUser.getUid(), new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    userRole = (String) userData.get("userType");
                    // Mostrar u ocultar el FAB según el rol
                    if ("Administrador".equals(userRole) || "Personal_Administrativo".equals(userRole)) {
                        binding.fabAddEvento.setVisibility(View.VISIBLE);
                    } else {
                        binding.fabAddEvento.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    // Ocultar el FAB si no se puede obtener el rol
                    binding.fabAddEvento.setVisibility(View.GONE);
                }
            });
        }
    }

    private void handleFabClick() {
        if ("Administrador".equals(userRole) || "Personal_Administrativo".equals(userRole)) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CreateEventFragment())
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
                eventosAdapter.setEventos(events);
                showEmptyState(events.isEmpty());
            }

            @Override
            public void onCancelled(String message) {
                Toast.makeText(getContext(), "Error al cargar eventos: " + message, Toast.LENGTH_SHORT).show();
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
                    .replace(R.id.fragment_container, nextFragment)
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
