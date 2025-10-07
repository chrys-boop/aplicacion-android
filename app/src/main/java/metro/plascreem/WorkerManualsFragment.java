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

    private static final String DATABASE_PATH = "manuales_pdf";

    private RecyclerView recyclerViewManuals;
    private ProgressBar progressBarManuals;
    private FileAdapter fileAdapter;
    private final List<FileMetadata> manualList = new ArrayList<>();
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_worker_manuals, container, false);

        databaseReference = FirebaseDatabase.getInstance().getReference(DATABASE_PATH);

        recyclerViewManuals = view.findViewById(R.id.recycler_view_manuals);
        progressBarManuals = view.findViewById(R.id.progress_bar_manuals);

        setupRecyclerView();
        loadManuals();

        return view;
    }

    private void setupRecyclerView() {
        recyclerViewManuals.setLayoutManager(new LinearLayoutManager(getContext()));
        // El 'false' es crucial para ocultar el botón de eliminar
        fileAdapter = new FileAdapter(manualList, this, false);
        recyclerViewManuals.setAdapter(fileAdapter);
    }

    private void loadManuals() {
        progressBarManuals.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                manualList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    FileMetadata metadata = postSnapshot.getValue(FileMetadata.class);
                    manualList.add(metadata);
                }
                fileAdapter.notifyDataSetChanged();
                progressBarManuals.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar los manuales.", Toast.LENGTH_SHORT).show();
                progressBarManuals.setVisibility(View.GONE);
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
        // No hacer nada aquí. El trabajador no puede eliminar archivos.
    }
}
