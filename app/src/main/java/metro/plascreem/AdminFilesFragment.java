package metro.plascreem;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer;


public class AdminFilesFragment extends Fragment implements FileAdapter.OnFileActionListener {

    private static final String TAG = "AdminFilesFragment";
    private static final String DATABASE_PATH = "files"; // Ruta en Firebase Realtime DB
    private static final String STORAGE_FOLDER = "archivos_eventos"; // Carpeta en Supabase

    private Button btnSelectFile, btnUploadFile;
    private TextView tvFileNameStatus;
    private ProgressBar progressBarUpload, progressBarList;
    private RecyclerView recyclerViewFiles;

    private Uri selectedFileUri;
    private String selectedFileName;

    private DatabaseReference databaseReference;
    private FileAdapter fileAdapter;
    private final List<FileMetadata> fileList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    selectedFileName = getFileNameFromUri(selectedFileUri);
                    tvFileNameStatus.setText(selectedFileName);
                    btnUploadFile.setEnabled(true);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_files, container, false);

        databaseReference = FirebaseDatabase.getInstance("https://capacitacion-material-default-rtdb.firebaseio.com/").getReference(DATABASE_PATH);

        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnUploadFile = view.findViewById(R.id.btn_upload_file);
        tvFileNameStatus = view.findViewById(R.id.tv_file_name_status);
        progressBarUpload = view.findViewById(R.id.progress_bar_upload);
        progressBarList = view.findViewById(R.id.progress_bar_list);
        recyclerViewFiles = view.findViewById(R.id.recycler_view_files);

        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        fileAdapter = new FileAdapter(fileList, this, true);
        recyclerViewFiles.setAdapter(fileAdapter);

        btnSelectFile.setOnClickListener(v -> openFilePicker());
        btnUploadFile.setOnClickListener(v -> uploadFileToSupabase());

        loadFilesFromDatabase();

        return view;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void uploadFileToSupabase() {
        if (selectedFileUri == null || selectedFileName == null) {
            Toast.makeText(getContext(), "Por favor, seleccione un archivo primero", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarUpload.setVisibility(View.VISIBLE);
        btnUploadFile.setEnabled(false);
        tvFileNameStatus.setText("Subiendo " + selectedFileName + "...");

        // Limpiar el nombre del archivo para que sea seguro para Supabase
        String sanitizedFileName = sanitizeFileName(selectedFileName);
        String storagePath = STORAGE_FOLDER + "/" + sanitizedFileName;

        SupabaseManager.uploadFile(getContext(), selectedFileUri, storagePath, new SupabaseUploadListener() {
            @Override
            public void onSuccess(String publicUrl) {
                Toast.makeText(getContext(), "Archivo subido con éxito", Toast.LENGTH_SHORT).show();

                String fileId = databaseReference.push().getKey();

                // Creamos los metadatos con el nombre original y la ruta sanitizada
                FileMetadata metadata = new FileMetadata(
                        fileId,
                        selectedFileName, // El nombre original para mostrar
                        publicUrl,
                        storagePath,      // La ruta con el nombre sanitizado
                        null,
                        0,
                        System.currentTimeMillis()
                );

                if (fileId != null) {
                    databaseReference.child(fileId).setValue(metadata)
                            .addOnSuccessListener(aVoid -> resetUploadUI())
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error al guardar en DB: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                resetUploadUI();
                            });
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al subir el archivo: " + message, Toast.LENGTH_LONG).show();
                resetUploadUI();
            }
        });
    }

    private void resetUploadUI() {
        selectedFileUri = null;
        selectedFileName = null;
        progressBarUpload.setVisibility(View.GONE);
        btnUploadFile.setEnabled(false);
        tvFileNameStatus.setText("Ningún archivo seleccionado");
    }

    private void loadFilesFromDatabase() {
        progressBarList.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fileList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    FileMetadata metadata = postSnapshot.getValue(FileMetadata.class);
                    if (metadata != null) {
                        metadata.setFileId(postSnapshot.getKey());
                        fileList.add(metadata);
                    }
                }
                fileAdapter.notifyDataSetChanged();
                progressBarList.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar la lista de archivos.", Toast.LENGTH_SHORT).show();
                progressBarList.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onViewFile(FileMetadata file) {
        if (file.getDownloadUrl() == null || file.getDownloadUrl().isEmpty()) {
            Toast.makeText(getContext(), "URL del archivo no disponible.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getDownloadUrl()));
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontro una aplicación para abrir este tipo de archivo.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDownloadFile(FileMetadata file) {
        if (getContext() == null || file.getDownloadUrl() == null || file.getDownloadUrl().isEmpty()) {
            Toast.makeText(getContext(), "No se puede iniciar la descarga.", Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(file.getDownloadUrl()));
        request.setTitle(file.getFileName());
        request.setDescription("Descargando archivo...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.getFileName());

        DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(getContext(), "Descarga iniciada...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error al iniciar el servicio de descarga.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteFile(FileMetadata file) {
        if (file.getFileId() == null || file.getStoragePath() == null || file.getStoragePath().isEmpty()) {
            Toast.makeText(getContext(), "Metadatos del archivo incompletos. No se puede eliminar.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar '" + file.getFileName() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    SupabaseManager.deleteFile(file.getStoragePath(), new SupabaseDeleteListener() {
                        @Override
                        public void onSuccess() {
                            databaseReference.child(file.getFileId()).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Archivo eliminado.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al eliminar de la base de datos.", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(getContext(), "Error al eliminar de Storage: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

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
        }
        return result;
    }

    // Nuevo método para limpiar el nombre del archivo para que sea seguro para la URL
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unknown_file_" + System.currentTimeMillis();
        // Normaliza el texto para separar acentos de las letras
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFD);
        // Elimina los caracteres de acentos
        String noAccents = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        // Reemplaza espacios con guiones bajos y elimina otros caracteres no válidos
        return noAccents.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
    }
}
