package metro.plascreem;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class AdminFilesFragment extends Fragment implements FileAdapter.OnFileActionListener {

    private static final String DATABASE_PATH = "manuales_pdf";

    private Button btnSelectFile, btnUploadFile;
    private TextView tvFileNameStatus;
    private ProgressBar progressBarUpload, progressBarList;
    private RecyclerView recyclerViewFiles;

    private Uri selectedFileUri;
    private String selectedFileName;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private FileAdapter fileAdapter;
    private final List<FileMetadata> fileList = new ArrayList<>();

    // Launcher para el selector de archivos
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

        // Inicializar Firebase
        storageReference = FirebaseStorage.getInstance().getReference(DATABASE_PATH);
        databaseReference = FirebaseDatabase.getInstance().getReference(DATABASE_PATH);

        // Vincular vistas
        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnUploadFile = view.findViewById(R.id.btn_upload_file);
        tvFileNameStatus = view.findViewById(R.id.tv_file_name_status);
        progressBarUpload = view.findViewById(R.id.progress_bar_upload);
        progressBarList = view.findViewById(R.id.progress_bar_list);
        recyclerViewFiles = view.findViewById(R.id.recycler_view_files);

        // Configurar RecyclerView
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        fileAdapter = new FileAdapter(fileList, this);
        recyclerViewFiles.setAdapter(fileAdapter);

        // Configurar listeners
        btnSelectFile.setOnClickListener(v -> openFilePicker());
        btnUploadFile.setOnClickListener(v -> uploadFile());

        // Cargar lista de archivos
        loadFilesFromDatabase();

        return view;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void uploadFile() {
        if (selectedFileUri == null || selectedFileName == null) {
            Toast.makeText(getContext(), "Por favor, seleccione un archivo primero", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarUpload.setVisibility(View.VISIBLE);
        btnUploadFile.setEnabled(false);

        StorageReference fileRef = storageReference.child(selectedFileName);

        fileRef.putFile(selectedFileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Toast.makeText(getContext(), "Archivo subido con éxito", Toast.LENGTH_SHORT).show();
                    FileMetadata metadata = new FileMetadata(selectedFileName, uri.toString(), fileRef.getPath());
                    String fileId = databaseReference.push().getKey();
                    if (fileId != null) {
                        databaseReference.child(fileId).setValue(metadata);
                    }
                    resetUploadUI();
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al subir el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetUploadUI();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBarUpload.setProgress((int) progress);
                });
    }

    private void loadFilesFromDatabase() {
        progressBarList.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fileList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    FileMetadata metadata = postSnapshot.getValue(FileMetadata.class);
                    fileList.add(metadata);
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
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(file.getUrl()), "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "No se encontró una aplicación para abrir PDFs", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteFile(FileMetadata file) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar '" + file.getName() + "'? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Eliminar de Firebase Storage
                    StorageReference fileRefToDelete = FirebaseStorage.getInstance().getReferenceFromUrl(file.getUrl());
                    fileRefToDelete.delete().addOnSuccessListener(aVoid -> {
                        // Eliminar de Firebase Realtime Database
                        databaseReference.orderByChild("url").equalTo(file.getUrl()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    snapshot.getRef().removeValue();
                                }
                                Toast.makeText(getContext(), "Archivo eliminado con éxito", Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(getContext(), "Error al eliminar de la base de datos", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).addOnFailureListener(e -> Toast.makeText(getContext(), "Error al eliminar el archivo de Storage", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
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

    private void resetUploadUI() {
        selectedFileUri = null;
        selectedFileName = null;
        progressBarUpload.setVisibility(View.GONE);
        progressBarUpload.setProgress(0);
        btnUploadFile.setEnabled(false);
        tvFileNameStatus.setText("Ningún archivo seleccionado");
    }
}
