package metro.plascreem;

import android.app.Activity;
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

import metro.plascreem.databinding.FragmentUploadDocumentsBinding;
import static android.app.Activity.RESULT_OK;

public class UploadDocumentsFragment extends Fragment {

    // Usaremos un único Request Code
    private static final int PICK_DOCUMENT_REQUEST = 1;

    private FragmentUploadDocumentsBinding binding;
    private Uri selectedFileUri = null; // URI del archivo seleccionado

    public UploadDocumentsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Usar View Binding para inflar el layout
        binding = FragmentUploadDocumentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Configurar el botón ÚNICO (Seleccionar y Subir)
        // El View Binding convierte 'btn_subir_documento' a 'btnSubirDocumento'
        binding.btnSubirDocumento.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                // Si no hay archivo seleccionado, abre el selector (Selección)
                openFileSelector();
            } else {
                // Si ya hay un archivo, inicia la subida (Subida)
                uploadFileToStorage(selectedFileUri);
            }
        });

        // Inicializar el estado del texto
        binding.tvFileStatus.setText("Estado: Ningún archivo en cola.");

        // Al inicio, forzamos la apertura del selector
        if (selectedFileUri == null) {
           // openFileSelector();
        }
    }

    // Método para abrir el selector de archivos
    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Informar que el botón ahora está en modo "Seleccionar"
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

    // Método para manejar el proceso de subida
    private void uploadFileToStorage(Uri fileUri) {
        // 1. Actualizar la UI e inhabilitar el botón
        binding.tvFileStatus.setText("Subiendo archivo...");
        binding.btnSubirDocumento.setEnabled(false);

        // 2. TODO: LÓGICA DE CONEXIÓN REAL A SUPABASE/FIREBASE AQUÍ
        // Usar 'fileUri' para leer el archivo y subirlo.

        // Simulación de subida (Después de 3 segundos):
        Toast.makeText(getContext(), "Subida iniciada para: " + fileUri.getLastPathSegment(), Toast.LENGTH_LONG).show();

        // Tras la subida exitosa:
        // Toast.makeText(getContext(), "Documento subido con éxito.", Toast.LENGTH_SHORT).show();
        // getParentFragmentManager().popBackStack(); // Volver al Calendario

        // Simulación de reset después de una subida (quitar esto en la versión final)
        resetUploadState();

    }

    /**
     * Maneja el resultado del selector de archivos.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_DOCUMENT_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                selectedFileUri = data.getData();

                // Actualizar la UI con el nombre del archivo seleccionado
                String fileName = selectedFileUri.getLastPathSegment();
                binding.tvFileStatus.setText("Archivo listo. Presione de nuevo para subir: " + fileName);

                // Actualizar el texto del botón al modo "Subir"
                binding.btnSubirDocumento.setText("Iniciar Subida a la Nube");
                binding.btnSubirDocumento.setEnabled(true);

            } else {
                // Selección cancelada o fallida
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