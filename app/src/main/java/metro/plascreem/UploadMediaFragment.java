package metro.plascreem;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import metro.plascreem.databinding.FragmentUploadMediaBinding;

public class UploadMediaFragment extends Fragment {

    private FragmentUploadMediaBinding binding;
    private Uri selectedFileUri = null;
    private String selectedFileName = null;
    private DatabaseManager databaseManager;

    // ActivityResultLauncher para el selector de archivos (método moderno)
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    selectedFileName = getFileNameFromUri(selectedFileUri);

                    // Actualizar UI con el archivo seleccionado
                    binding.tvMediaStatus.setText("Listo para subir: " + selectedFileName);
                    binding.btnSubirMedia.setText("Iniciar Subida");
                    binding.btnSubirMedia.setEnabled(true);
                } else {
                    Toast.makeText(getContext(), "Selección de archivo cancelada.", Toast.LENGTH_SHORT).show();
                    resetUploadState();
                }
            });

    public UploadMediaFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUploadMediaBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSubirMedia.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                openMediaSelector();
            } else {
                uploadMediaFile();
            }
        });
    }

    private void openMediaSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*|video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        mediaPickerLauncher.launch(Intent.createChooser(intent, "Seleccionar Foto o Video"));
    }

    private void uploadMediaFile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (selectedFileUri == null || selectedFileName == null) {
            Toast.makeText(getContext(), "No hay archivo seleccionado.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inhabilitar UI para la subida
        binding.btnSubirMedia.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        String uploaderId = currentUser.getUid();

        // Llamada al método estandarizado que se encarga de todo
        databaseManager.uploadFile(selectedFileUri, selectedFileName, uploaderId, new DatabaseManager.UploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                Toast.makeText(getContext(), "Archivo subido con éxito.", Toast.LENGTH_SHORT).show();
                // La subida fue exitosa, volver al fragmento anterior (Calendario)
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al subir: " + message, Toast.LENGTH_LONG).show();
                resetUploadState(); // Permitir reintentar
            }

            @Override
            public void onProgress(double progress) {
                binding.progressBar.setProgress((int) progress);
                binding.tvMediaStatus.setText(String.format(java.util.Locale.getDefault(), "Subiendo... %.1f%%", progress));
            }
        });
    }

    // Función de ayuda para obtener el nombre de archivo de forma robusta
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "unknown_file";
    }

    private void resetUploadState() {
        selectedFileUri = null;
        selectedFileName = null;
        binding.progressBar.setVisibility(View.GONE);
        binding.progressBar.setProgress(0);
        binding.tvMediaStatus.setText("Estado: 0 archivos listos para subir.");
        binding.btnSubirMedia.setText("Seleccionar Foto o Video");
        binding.btnSubirMedia.setEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevenir fugas de memoria
    }
}

