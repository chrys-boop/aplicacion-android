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

import metro.plascreem.databinding.FragmentUploadMediaBinding; // Usaremos esta clase
import static android.app.Activity.RESULT_OK;

public class UploadMediaFragment extends Fragment {

    private static final int PICK_MEDIA_REQUEST = 2;
    private FragmentUploadMediaBinding binding; // Variable de View Binding
    private Uri selectedFileUri = null; // URI del archivo seleccionado
    private DatabaseManager databaseManager;

    public UploadMediaFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Usar View Binding para inflar el layout
        binding = FragmentUploadMediaBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Configurar el botón de selección/subida
        // El ID 'btn_subir_media' se convierte a 'btnSubirMedia'
        binding.btnSubirMedia.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                // Si no hay archivo, abrir el selector
                openMediaSelector();
            } else {
                // Si ya hay un archivo, iniciar la subida
                uploadMediaToStorage(selectedFileUri);
            }
        });

        // Estado inicial del botón (se inicializa con el texto del XML)
    }

    /**
     * Lanza el Intent para abrir el selector de fotos y videos del sistema.
     */
    private void openMediaSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Filtro para aceptar imágenes y videos
        intent.setType("image/*|video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        binding.btnSubirMedia.setText("Seleccionar Foto o Video"); // Restablece el texto al modo selección

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Seleccionar Foto o Video"),
                    PICK_MEDIA_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "Error: No se encontró una aplicación para seleccionar multimedia.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Simulación de la función de subida a Supabase/Firebase.
     */
    private void uploadMediaToStorage(Uri fileUri) {
        // 1. Actualizar la UI e inhabilitar el botón
        binding.tvMediaStatus.setText("Subiendo archivo multimedia...");
        binding.btnSubirMedia.setEnabled(false);

        String fileName = selectedFileUri.getLastPathSegment();
        databaseManager.uploadMediaFile(fileUri, fileName, new DatabaseManager.UploadListener() {
            @Override
            public void onSuccess(String downloadUrl) {
                long fileSize = 1024; // You can get the file size here
                String uploaderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                databaseManager.saveFileMetadata(fileName, downloadUrl, fileSize, uploaderId, new DatabaseManager.DataSaveListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Multimedia subido con éxito.", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack(); // Volver al Calendario
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(getContext(), "Error al guardar los metadatos: " + message, Toast.LENGTH_LONG).show();
                        resetUploadState();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al subir el multimedia: " + message, Toast.LENGTH_LONG).show();
                resetUploadState();
            }

            @Override
            public void onProgress(double progress) {
                binding.tvMediaStatus.setText(String.format("Subiendo... %.2f%%", progress));
            }
        });
    }

    /**
     * Este método recibe la respuesta después de que el usuario selecciona un archivo.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_MEDIA_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                selectedFileUri = data.getData();

                // Actualizar la UI con el nombre del archivo seleccionado
                String fileName = selectedFileUri.getLastPathSegment();
                binding.tvMediaStatus.setText("Media lista. Presione el botón para subir: " + fileName);

                // Actualizar el texto del botón al modo "Subir"
                binding.btnSubirMedia.setText("Iniciar Subida a la Nube");
                binding.btnSubirMedia.setEnabled(true);

            } else {
                // Selección cancelada o fallida
                resetUploadState();
                Toast.makeText(getContext(), "Selección de archivo cancelada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resetUploadState() {
        selectedFileUri = null;
        binding.tvMediaStatus.setText("Estado: 0 archivos listos para subir.");
        binding.btnSubirMedia.setText("Seleccionar Foto o Video");
        binding.btnSubirMedia.setEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
