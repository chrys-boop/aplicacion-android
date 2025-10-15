
package metro.plascreem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metro.plascreem.databinding.FragmentAdminTrackingBinding;

public class AdminTrackingFragment extends Fragment implements HistoricoAdapter.OnFileInteractionListener {

    private static final String TAG = "AdminTrackingFragment";
    private static final String TEMPLATE_FOLDER_PATH = "plantillas/";

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
        databaseManager = new DatabaseManager(getContext()); // Pasar contexto si es necesario
        storageManager = new StorageManager();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSpinner();
        loadUsers();
        setupTemplateButtons(); // NUEVO: Configurar botones de plantillas
    }

    // --- NUEVO MÉTODO PARA CONFIGURAR LOS BOTONES DE PLANTILLAS ---
    private void setupTemplateButtons() {
        // Un solo botón para abrir el menú de plantillas
        binding.btnPlantillaFormato.setText("Ver Plantillas y Manuales");
        binding.btnPlantillaFormato.setOnClickListener(v -> showTemplateSelectionDialog());

        // Ocultar el segundo botón que ya no se necesita
        binding.btnPlantillaDiagrama.setVisibility(View.GONE);
    }

    // --- NUEVO MÉTODO PARA MOSTRAR EL DIÁLOGO DE SELECCIÓN ---
    private void showTemplateSelectionDialog() {
        setLoadingState(true);
        storageManager.listFilesInFolder(TEMPLATE_FOLDER_PATH, new StorageManager.FileListListener() {
            @Override
            public void onSuccess(List<StorageReference> files) {
                setLoadingState(false);
                if (getContext() == null || files.isEmpty()) {
                    Toast.makeText(getContext(), "No se encontraron plantillas.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Extraer los nombres de los archivos para mostrarlos en la lista
                List<String> fileNames = new ArrayList<>();
                for (StorageReference fileRef : files) {
                    fileNames.add(fileRef.getName());
                }

                // Crear y mostrar el diálogo de selección
                new AlertDialog.Builder(getContext())
                        .setTitle("Seleccionar Plantilla")
                        .setItems(fileNames.toArray(new CharSequence[0]), (dialog, which) -> {
                            // Cuando el usuario selecciona un archivo, iniciar la descarga
                            String selectedFileName = fileNames.get(which);
                            downloadAndOpenFile(selectedFileName);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            @Override
            public void onFailure(String error) {
                setLoadingState(false);
                Log.e(TAG, "Error al listar plantillas: " + error);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar plantillas: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // --- NUEVO MÉTODO PARA DESCARGAR Y ABRIR EL ARCHIVO SELECCIONADO ---
    private void downloadAndOpenFile(String fileName) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Iniciando descarga de " + fileName + "...", Toast.LENGTH_SHORT).show();
        setLoadingState(true);

        String fullPath = TEMPLATE_FOLDER_PATH + fileName;
        storageManager.getDownloadUrl(fullPath, new StorageManager.DownloadUrlListener() {
            @Override
            public void onSuccess(String url) {
                setLoadingState(false);
                // Abrir la URL en un navegador o visor de PDF
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Log.e(TAG, "No se pudo abrir la URL: " + url, e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "No se encontró una aplicación para abrir el archivo.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                setLoadingState(false);
                Log.e(TAG, "Error al obtener URL de descarga: " + error);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "No se pudo descargar el archivo: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupRecyclerView() {
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
                setLoadingState(false);
                if (getContext() == null) return;
                userList.clear();
                User hintUser = new User();
                hintUser.setNombreCompleto("Seleccionar un usuario...");
                userList.add(hintUser);
                userList.addAll(users);

                spinnerAdapter.clear();
                spinnerAdapter.addAll(userList);
                spinnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(String message) {
                setLoadingState(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar usuarios: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadUserFileHistory(String userId) {
        setLoadingState(true);
        databaseManager.getFilesUploadedByUser(userId, new DatabaseManager.FileHistoryListener() {
            @Override
            public void onHistoryReceived(List<FileMetadata> history) {
                setLoadingState(false);
                if (getContext() == null) return;

                fileHistoryList.clear();
                fileHistoryList.addAll(history);
                Collections.sort(fileHistoryList, (f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()));

                historicoAdapter.notifyDataSetChanged();

                if (history.isEmpty()) {
                    Toast.makeText(getContext(), "Este usuario no tiene archivos subidos.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(String message) {
                setLoadingState(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al cargar el historial: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (binding != null) {
            binding.progressBarAdmin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onViewFile(FileMetadata file) {
        if (file.getDownloadUrl() != null && !file.getDownloadUrl().isEmpty()){
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getDownloadUrl()));
            try {
                startActivity(browserIntent);
            } catch (Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "No se pudo abrir el enlace.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "URL no disponible.", Toast.LENGTH_SHORT).show();
            }
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
                    storageManager.deleteFile(file.getStoragePath(), new StorageManager.StorageListener() {
                        @Override
                        public void onSuccess(String message) {
                            databaseManager.deleteFileRecord(file.getFileId(), new DatabaseManager.DatabaseListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Archivo eliminado correctamente", Toast.LENGTH_SHORT).show();
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
