package metro.plascreem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.util.Collections;
import java.util.List;

public class WorkerActivityHistoryFragment extends Fragment {

    private RecyclerView rvActivityHistory;
    private ActivityHistoryAdapter adapter;
    private List<HistoricoArchivo> historyList;
    private ProgressBar progressBar;
    private TextView tvNoHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_worker_activity_history, container, false);

        rvActivityHistory = view.findViewById(R.id.rv_activity_history);
        progressBar = view.findViewById(R.id.progress_bar_activity_history);
        tvNoHistory = view.findViewById(R.id.tv_no_activity_history);

        setupRecyclerView();
        fetchActivityHistory();

        return view;
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        adapter = new ActivityHistoryAdapter(historyList);
        rvActivityHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActivityHistory.setAdapter(adapter);
    }

    private void fetchActivityHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);
        rvActivityHistory.setVisibility(View.GONE);

        // CORRECCIÓN: Apuntar a la ruta correcta en la base de datos
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("file_download_history");

        historyRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                historyList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        HistoricoArchivo historyItem = snapshot.getValue(HistoricoArchivo.class);
                        if (historyItem != null) {
                            historyList.add(historyItem);
                        }
                    }
                    // Invertir la lista para mostrar lo más reciente primero
                    Collections.reverse(historyList);
                    adapter.notifyDataSetChanged();
                    tvNoHistory.setVisibility(View.GONE);
                    rvActivityHistory.setVisibility(View.VISIBLE);
                } else {
                    tvNoHistory.setVisibility(View.VISIBLE);
                    rvActivityHistory.setVisibility(View.GONE);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error al cargar el historial: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}


