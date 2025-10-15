package metro.plascreem;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metro.plascreem.databinding.FragmentEventListBinding;

public class EventListFragment extends Fragment implements EventosAdapter.EventoActionListener {

    private FragmentEventListBinding binding;
    private DatabaseManager databaseManager;
    private EventosAdapter eventosAdapter;
    private final List<Evento> allEvents = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventListBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager(getContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadAllEvents();
    }

    private void setupRecyclerView() {
        eventosAdapter = new EventosAdapter(allEvents, this);
        binding.rvEventList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEventList.setAdapter(eventosAdapter);
    }

    private void setLoadingState(boolean isLoading) {
        if (binding == null) return; // Evitar crash si la vista es destruida
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.rvEventList.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void loadAllEvents() {
        setLoadingState(true);
        databaseManager.getAllEvents(new DatabaseManager.EventsListener() {
            @Override
            public void onEventsReceived(List<Evento> events) {
                if (isAdded() && binding != null) {
                    setLoadingState(false);
                    Collections.reverse(events);
                    eventosAdapter.setEventos(events);
                    binding.emptyState.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(String message) {
                if (isAdded() && binding != null) {
                    setLoadingState(false);
                    Toast.makeText(getContext(), "Error al cargar eventos: " + message, Toast.LENGTH_SHORT).show();
                    binding.emptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // --- Implementación de la interfaz del adaptador ---

    @Override
    public void onActionClick(Evento evento) {
        Toast.makeText(getContext(), "Acción no disponible en esta vista de auditoría.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEventoClicked(Evento evento) {
        // --- ¡LÓGICA DE NAVEGACIÓN ACTUALIZADA! ---
        if (getActivity() != null) {
            EventDetailFragment detailFragment = EventDetailFragment.newInstance(evento);

            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.admin_fragment_container, detailFragment);
            transaction.addToBackStack(null); // Permite volver a la lista con el botón "Atrás"
            transaction.commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
