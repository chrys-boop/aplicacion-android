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

public class AdminFilesFragment extends Fragment implements FileAdapter.OnFileActionListener {

    private static final String TAG = "AdminFilesFragment";
    private static final String DATABASE_PATH = "files"; // Ruta en Firebase Realtime DB
    private static final String STORAGE_FOLDER = "plantillas"; // Carpeta en Supabase

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

        // Ya no se necesita Firebase Storage, solo la base de datos para metadatos
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
        btnUploadFile.setOnClickListener(v -> uploadFileToSupabase()); // Cambiado a m\u00e9todo de Supabase

        loadFilesFromDatabase();

        return view;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    // --- M\u00c9TODO DE SUBIDA ACTUALIZADO PARA USAR SUPABASE ---
    private void uploadFileToSupabase() {
        if (selectedFileUri == null || selectedFileName == null) {
            Toast.makeText(getContext(), "Por favor, seleccione un archivo primero", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarUpload.setVisibility(View.VISIBLE);
        btnUploadFile.setEnabled(false);
        tvFileNameStatus.setText("Subiendo " + selectedFileName + "...");

        String storagePath = STORAGE_FOLDER + "/" + selectedFileName;

        SupabaseManager.uploadFile(getContext(), selectedFileUri, storagePath, new SupabaseUploadListener() {
            @Override
            public void onSuccess(String publicUrl) {
                Toast.makeText(getContext(), "Archivo subido con \u00e9xito", Toast.LENGTH_SHORT).show();

                String fileId = databaseReference.push().getKey();

                // Creamos los metadatos con la informaci\u00f3n de Supabase
                FileMetadata metadata = new FileMetadata(
                        fileId,
                        selectedFileName,
                        publicUrl, // La URL p\u00fablica de Supabase
                        storagePath, // La ruta de almacenamiento en Supabase
                        null, // userId, no aplica para archivos de admin
                        0,    // size, se puede a\u00f1adir luego si es necesario
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
        tvFileNameStatus.setText("Ning\u00fan archivo seleccionado");
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
        // Esta funci\u00f3n ya funciona con cualquier URL p\u00fablica
        if (file.getDownloadUrl() == null || file.getDownloadUrl().isEmpty()) {
            Toast.makeText(getContext(), "URL del archivo no disponible.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getDownloadUrl()));
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontro una aplicaci\u00f3n para abrir este tipo de archivo.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDownloadFile(FileMetadata file) {
        // Esta funci\u00f3n ya funciona con cualquier URL p\u00fablica
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

    // --- M\u00c9TODO DE BORRADO ACTUALIZADO PARA USAR SUPABASE ---
    @Override
    public void onDeleteFile(FileMetadata file) {
        if (file.getFileId() == null || file.getStoragePath() == null || file.getStoragePath().isEmpty()) {
            Toast.makeText(getContext(), "Metadatos del archivo incompletos. No se puede eliminar.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminaci\u00f3n")
                .setMessage("\u00bfEst\u00e1s seguro de que quieres eliminar '" + file.getFileName() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // PRIMERO: Borrar de Supabase Storage usando el path guardado
                    SupabaseManager.deleteFile(file.getStoragePath(), new SupabaseDeleteListener() {
                        @Override
                        public void onSuccess() {
                            // SEGUNDO: Si se borra de Supabase, borrar el registro de Firebase DB
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
}
