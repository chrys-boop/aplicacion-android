package metro.plascreem;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class UserListFragment extends Fragment implements UserAdapter.OnUserClickListener {

    private RecyclerView usersRecyclerView;
    private UserAdapter userAdapter;
    private DatabaseManager databaseManager;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private Spinner roleSpinner;

    private CardView userDetailCardView;
    private TextView detailNameTextView, detailEmailTextView, detailRoleTextView, detailExpedienteTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        databaseManager = new DatabaseManager(getContext());

        usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(new ArrayList<>(), this);
        usersRecyclerView.setAdapter(userAdapter);

        setupRoleSpinner();
    }

    private void initializeViews(@NonNull View view) {
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        usersRecyclerView = view.findViewById(R.id.usersRecyclerView);
        roleSpinner = view.findViewById(R.id.roleSpinner);

        userDetailCardView = view.findViewById(R.id.userDetailCardView);
        detailNameTextView = view.findViewById(R.id.detailNameTextView);
        detailEmailTextView = view.findViewById(R.id.detailEmailTextView);
        detailRoleTextView = view.findViewById(R.id.detailRoleTextView);
        detailExpedienteTextView = view.findViewById(R.id.detailExpedienteTextView);
    }

    private void setupRoleSpinner() {
        // CORRECCIÓN: Usar el nuevo array de recursos que creamos
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.user_roles_for_filter, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = (String) parent.getItemAtPosition(position);
                loadUsersByRole(selectedRole);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadUsersByRole(String role) {
        showLoading(true);
        userDetailCardView.setVisibility(View.GONE);

        DatabaseManager.DataCallback<List<User>> callback = new DatabaseManager.DataCallback<List<User>>() {
            @Override
            public void onDataReceived(List<User> users) {
                if (isAdded()) {
                    showLoading(false);
                    if (users == null || users.isEmpty()) {
                        showEmptyState(true);
                        userAdapter.setUsers(new ArrayList<>());
                    } else {
                        showEmptyState(false);
                        userAdapter.setUsers(users);
                    }
                }
            }

            @Override
            public void onDataCancelled(String message) {
                if (isAdded()) {
                    showLoading(false);
                    showEmptyState(true);
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                }
            }
        };

        // Lógica para "Todos" vs. un rol específico
        if ("Todos".equals(role)) {
            databaseManager.getAllUsers(callback); // Llamará al nuevo método que te proporcionaré
        } else {
            databaseManager.getUsersByRole(role, callback); // Usa el método que ya existe
        }
    }

    @Override
    public void onUserClick(User user) {
        userDetailCardView.setVisibility(View.VISIBLE);
        detailNameTextView.setText("Nombre: " + (user.getNombreCompleto() != null ? user.getNombreCompleto() : "N/A"));
        detailEmailTextView.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "N/A"));
        detailRoleTextView.setText("Rol: " + (user.getUserType() != null ? user.getUserType() : "N/A"));
        detailExpedienteTextView.setText("Expediente: " + (user.getNumeroExpediente() != null ? user.getNumeroExpediente() : "N/A"));
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        usersRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyStateTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        usersRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}

