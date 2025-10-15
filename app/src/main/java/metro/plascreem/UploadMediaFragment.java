
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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import metro.plascreem.databinding.FragmentUploadMediaBinding;

public class UploadMediaFragment extends Fragment {

    private FragmentUploadMediaBinding binding;
    private Uri selectedFileUri = null;
    private String selectedFileName = null;
    private DatabaseManager databaseManager;

    // ActivityResultLauncher for the file picker (modern method)
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    selectedFileName = getFileNameFromUri(selectedFileUri);

                    // Update UI with the selected file
                    binding.tvMediaStatus.setText("Listo para subir: " + selectedFileName);
                    binding.btnSubirMedia.setText("Iniciar Subida");
                    binding.btnSubirMedia.setEnabled(true);
                } else {
                    // Using the custom Toast
                    ToastUtils.showShortToast(getActivity(), "Selección de archivo cancelada.");
                    resetUploadState();
                }
            });

    public UploadMediaFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUploadMediaBinding.inflate(inflater, container, false);
        // Correctly initialize DatabaseManager with the required context.
        databaseManager = new DatabaseManager(getContext());
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
            ToastUtils.showShortToast(getActivity(), "No hay archivo seleccionado.");
            return;
        }
        if (currentUser == null) {
            ToastUtils.showShortToast(getActivity(), "Error: Usuario no autenticado.");
            return;
        }

        // Disable UI for upload
        binding.btnSubirMedia.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

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

        // AUDITORÍA: Final variables needed for the inner class
        final String finalUploaderName = uploaderName;
        final String finalFileName = selectedFileName;

        // Updated call to uploadFile, now including the uploader's name.
        databaseManager.uploadFile(selectedFileUri, selectedFileName, uploaderId, uploaderName, new DatabaseManager.UploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                ToastUtils.showShortToast(getActivity(), "Archivo subido con éxito.");

                // AUDITORÍA: Llamar a la función de Netlify para notificar la subida.
                sendAuditNotification(finalUploaderName, finalFileName, "Media");

                // Upload was successful, go back to the previous fragment (Calendar)
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(String message) {
                ToastUtils.showLongToast(getActivity(), "Error al subir: " + message);
                resetUploadState(); // Allow retry
            }

            @Override
            public void onProgress(double progress) {
                binding.progressBar.setProgress((int) progress);
                binding.tvMediaStatus.setText(String.format(java.util.Locale.getDefault(), "Subiendo... %.1f%%", progress));
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

    // Helper function to get the file name robustly
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
        binding = null; // Prevent memory leaks
    }
}

