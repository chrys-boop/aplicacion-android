package metro.plascreem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class WorkerManualsFragment extends Fragment implements FileAdapter.OnFileActionListener {

    private static final String DATABASE_PATH = "uploads";

    private RecyclerView recyclerViewFiles;
    private ProgressBar progressBar;
    private FileAdapter fileAdapter;
    private final List<FileMetadata> fileList = new ArrayList<>();
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_worker_manuals, container, false);

        databaseReference = FirebaseDatabase.getInstance().getReference(DATABASE_PATH);

        recyclerViewFiles = view.findViewById(R.id.recycler_view_manuals);
        progressBar = view.findViewById(R.id.progress_bar_manuals);

        setupRecyclerView();
        loadFiles();

        return view;
    }

    private void setupRecyclerView() {
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        fileAdapter = new FileAdapter(fileList, this, false);
        recyclerViewFiles.setAdapter(fileAdapter);
    }

    private void loadFiles() {
        progressBar.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    fileList.clear();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        FileMetadata metadata = postSnapshot.getValue(FileMetadata.class);
                        // ¡NUEVO! Filtrar para que solo se muestren los archivos PDF.
                        if (metadata != null && metadata.getUrl() != null &&
                                metadata.getUrl().toLowerCase().endsWith(".pdf")) {
                            fileList.add(metadata);
                        }
                    }
                    fileAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error al cargar los archivos.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onViewFile(FileMetadata file) {
        if (file == null || file.getUrl() == null || file.getUrl().isEmpty()) {
            Toast.makeText(getContext(), "URL del archivo no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simplificado: ya que solo mostramos PDFs, el tipo MIME es conocido.
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
        // La lógica para eliminar no se implementa aquí, ya que los trabajadores no pueden borrar.
    }
}
