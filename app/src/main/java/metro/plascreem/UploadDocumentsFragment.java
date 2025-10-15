
package metro.plascreem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

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
        // Correctly initialize DatabaseManager with the required context.
        databaseManager = new DatabaseManager(getContext());
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
        // Get the user's display name for the notification.
        String uploaderName = currentUser.getDisplayName();
        if (uploaderName == null || uploaderName.isEmpty()) {
            // Use email as a fallback if the display name is not set.
            uploaderName = currentUser.getEmail();
            if (uploaderName == null || uploaderName.isEmpty()) {
                uploaderName = "Usuario Desconocido"; // Final fallback.
            }
        }

        String fileName = selectedFileUri.getLastPathSegment();
        if (fileName == null) {
            fileName = "unknown_file_" + System.currentTimeMillis();
        }

        // AUDITORÍA: Final variables needed for the inner class
        final String finalUploaderName = uploaderName;
        final String finalFileName = fileName;

        // Updated call to uploadFile, now including the uploader's name.
        databaseManager.uploadFile(fileUri, fileName, uploaderId, uploaderName, new DatabaseManager.UploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                Toast.makeText(getContext(), "Documento subido con éxito.", Toast.LENGTH_SHORT).show();

                // AUDITORÍA: Llamar a la función de Netlify para notificar la subida.
                sendAuditNotification(finalUploaderName, finalFileName, "Documento");

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

    // AUDITORÍA: Nuevo método para enviar la notificación de auditoría.
    private void sendAuditNotification(String uploaderName, String fileName, String fileType) {
        if (getContext() == null) {
            Log.e("AuditError", "Context is null, cannot send audit notification.");
            return;
        }

        String auditUrl = "https://capacitacion-mrodante.netlify.app/.netlify/functions/send-audit-notification";

        JSONObject postData = new JSONObject();
        try {
            postData.put("userId", uploaderName);
            postData.put("action", "Subida de " + fileType);
            postData.put("details", "Archivo: " + fileName);
        } catch (JSONException e) {
            Log.e("AuditError", "Error creating audit JSON", e);
            return;
        }

        JsonObjectRequest auditRequest = new JsonObjectRequest(Request.Method.POST, auditUrl, postData,
                response -> Log.d("AuditSuccess", "Audit notification sent for " + fileName),
                error -> Log.e("AuditError", "Failed to send audit notification", error)
        );

        // Añadir la petición a la cola de Volley.
        Volley.newRequestQueue(getContext()).add(auditRequest);
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
