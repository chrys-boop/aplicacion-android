package metro.plascreem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import metro.plascreem.databinding.FragmentUploadDocumentsBinding;
import static android.app.Activity.RESULT_OK;

public class UploadDocumentsFragment extends Fragment {

    private static final int PICK_DOCUMENT_REQUEST = 1;

    private FragmentUploadDocumentsBinding binding;
    private Uri selectedFileUri = null;
    private DatabaseManager databaseManager;


    public UploadDocumentsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUploadDocumentsBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSubirDocumento.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                openFileSelector();
            } else {
                uploadFileToStorage(selectedFileUri);
            }
        });

        binding.tvFileStatus.setText("Estado: Ningún archivo en cola.");
    }

    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        binding.btnSubirDocumento.setText("Seleccionar Archivo y Subir");

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Seleccionar Documento"),
                    PICK_DOCUMENT_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "Error: No se encontró una aplicación para seleccionar archivos.", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadFileToStorage(Uri fileUri) {
        binding.tvFileStatus.setText("Subiendo archivo...");
        binding.btnSubirDocumento.setEnabled(false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_LONG).show();
            resetUploadState();
            return;
        }
        String uploaderId = currentUser.getUid();

        String fileName = selectedFileUri.getLastPathSegment();
        if (fileName == null) {
            fileName = "unknown_file_" + System.currentTimeMillis();
        }

        // Llamada corregida a uploadFile, ahora incluye el uploaderId
        databaseManager.uploadFile(fileUri, fileName, uploaderId, new DatabaseManager.UploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                // El guardado de metadatos ahora se hace dentro de DatabaseManager.
                // Aquí solo notificamos el éxito y volvemos.
                Toast.makeText(getContext(), "Documento subido con éxito.", Toast.LENGTH_SHORT).show();
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al subir el archivo: " + message, Toast.LENGTH_LONG).show();
                resetUploadState();
            }

            @Override
            public void onProgress(double progress) {
                binding.tvFileStatus.setText(String.format("Subiendo... %.2f%%", progress));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_DOCUMENT_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                selectedFileUri = data.getData();

                String fileName = selectedFileUri.getLastPathSegment();
                binding.tvFileStatus.setText("Archivo listo. Presione de nuevo para subir: " + fileName);
                binding.btnSubirDocumento.setText("Iniciar Subida a la Nube");
                binding.btnSubirDocumento.setEnabled(true);

            } else {
                resetUploadState();
            }
        }
    }

    private void resetUploadState() {
        selectedFileUri = null;
        binding.tvFileStatus.setText("Estado: Ningún archivo en cola.");
        binding.btnSubirDocumento.setText("Seleccionar Archivo y Subir");
        binding.btnSubirDocumento.setEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
