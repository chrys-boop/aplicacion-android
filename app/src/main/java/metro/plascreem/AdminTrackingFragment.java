package metro.plascreem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metro.plascreem.databinding.FragmentAdminTrackingBinding;

public class AdminTrackingFragment extends Fragment implements HistoricoAdapter.OnFileInteractionListener {

    private FragmentAdminTrackingBinding binding;
    private DatabaseManager databaseManager;
    private StorageManager storageManager;

    private List<User> userList = new ArrayList<>();
    private List<FileMetadata> fileHistoryList = new ArrayList<>();

    private ArrayAdapter<User> spinnerAdapter;
    private HistoricoAdapter historicoAdapter;

    public AdminTrackingFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminTrackingBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager(getContext());
        storageManager = new StorageManager(); // Inicializar StorageManager
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSpinner();
        loadUsers();

        // Listeners para plantillas (sin cambios)
        binding.btnPlantillaFormato.setOnClickListener(v ->
                Toast.makeText(getContext(), "Función de descarga de plantilla pendiente", Toast.LENGTH_SHORT).show());
        binding.btnPlantillaDiagrama.setOnClickListener(v ->
                Toast.makeText(getContext(), "Función de visualización de diagrama pendiente", Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        // El adaptador ahora requiere el listener, pasamos 'this'
        historicoAdapter = new HistoricoAdapter(fileHistoryList, this);
        binding.rvHistoricoArchivos.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvHistoricoArchivos.setAdapter(historicoAdapter);
        binding.rvHistoricoArchivos.setNestedScrollingEnabled(false);
    }

    private void setupSpinner() {
        spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSelectUser.setAdapter(spinnerAdapter);

        binding.spinnerSelectUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                User selectedUser = (User) parent.getItemAtPosition(position);
                if (selectedUser != null && selectedUser.getUid() != null) {
                    loadUserFileHistory(selectedUser.getUid());
                } else {
                    fileHistoryList.clear();
                    historicoAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUsers() {
        setLoadingState(true);
        databaseManager.getAllUsers(new DatabaseManager.AllUsersListener() {
            @Override
            public void onUsersReceived(List<User> users) {
                userList.clear();
                User hintUser = new User();
                hintUser.setNombreCompleto("Seleccionar un usuario...");
                userList.add(hintUser);
                userList.addAll(users);

                spinnerAdapter.clear();
                spinnerAdapter.addAll(userList);
                spinnerAdapter.notifyDataSetChanged();
                setLoadingState(false);
            }

            @Override
            public void onCancelled(String message) {
                Toast.makeText(getContext(), "Error al cargar usuarios: " + message, Toast.LENGTH_LONG).show();
                setLoadingState(false);
            }
        });
    }

    private void loadUserFileHistory(String userId) {
        setLoadingState(true);
        databaseManager.getFilesUploadedByUser(userId, new DatabaseManager.FileHistoryListener() {
            @Override
            public void onHistoryReceived(List<FileMetadata> history) {
                fileHistoryList.clear();
                fileHistoryList.addAll(history);
                // Ordenar por fecha descendente
                Collections.sort(fileHistoryList, (f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()));

                historicoAdapter.notifyDataSetChanged();
                setLoadingState(false);

                if (history.isEmpty()) {
                    Toast.makeText(getContext(), "Este usuario no tiene archivos subidos.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(String message) {
                Toast.makeText(getContext(), "Error al cargar el historial: " + message, Toast.LENGTH_LONG).show();
                setLoadingState(false);
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        binding.progressBarAdmin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // --- Implementación de OnFileInteractionListener ---

    @Override
    public void onViewFile(FileMetadata file) {
        if (file.getDownloadUrl() != null && !file.getDownloadUrl().isEmpty()){
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getDownloadUrl()));
            try {
                startActivity(browserIntent);
                Toast.makeText(getContext(), "Abriendo navegador...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "No se pudo abrir el enlace.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "URL no disponible.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteFile(final FileMetadata file) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Borrado")
                .setMessage("¿Estás seguro de que quieres eliminar el archivo '" + file.getFileName() + "'? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    setLoadingState(true);
                    // Llamada a StorageManager para borrar el archivo físico
                    storageManager.deleteFile(file.getStoragePath(), new StorageManager.StorageListener() {
                        @Override
                        public void onSuccess(String message) {
                            // Si se borra el físico, se borra el registro en la BD
                            databaseManager.deleteFileRecord(file.getFileId(), new DatabaseManager.DatabaseListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Archivo eliminado correctamente", Toast.LENGTH_SHORT).show();
                                    // Recargar la lista de archivos para el usuario actual
                                    if (binding.spinnerSelectUser.getSelectedItem() instanceof User) {
                                        User selectedUser = (User) binding.spinnerSelectUser.getSelectedItem();
                                        if (selectedUser != null && selectedUser.getUid() != null) {
                                            loadUserFileHistory(selectedUser.getUid());
                                        }
                                    }
                                    setLoadingState(false);
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(getContext(), "Error al eliminar registro: " + error, Toast.LENGTH_LONG).show();
                                    setLoadingState(false);
                                }
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(getContext(), "Error al eliminar archivo: " + error, Toast.LENGTH_LONG).show();
                            setLoadingState(false);
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

