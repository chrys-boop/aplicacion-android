package metro.plascreem;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import metro.plascreem.FileAdapter.OnFileActionListener;

public class WorkerManualsFragment extends Fragment {

    private static final String TAG = "WorkerManualsFragment";
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private List<FileMetadata> manualList;
    private DatabaseManager databaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_worker_manuals, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_manuals);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        manualList = new ArrayList<>();
        databaseManager = new DatabaseManager(getContext());

        // --- Listener con las nuevas acciones implementadas ---
        OnFileActionListener fileActionListener = new OnFileActionListener() {
            @Override
            public void onViewFile(FileMetadata file) {
                if (file.getDownloadUrl() == null || file.getDownloadUrl().isEmpty()) {
                    Toast.makeText(getContext(), R.string.error_file_url_missing, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Intenta abrir el archivo con una app nativa
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(file.getDownloadUrl()), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(getContext(), R.string.error_no_app_to_open_file, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDownloadFile(FileMetadata file) {
                if (file.getDownloadUrl() == null || file.getDownloadUrl().isEmpty()) {
                    Toast.makeText(getContext(), R.string.error_file_url_missing, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Lógica de descarga nativa
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(file.getDownloadUrl()));
                request.setTitle(file.getFileName());
                request.setDescription("Descargando manual...");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.getFileName());

                DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    downloadManager.enqueue(request);
                    Toast.makeText(getContext(), R.string.download_started, Toast.LENGTH_SHORT).show();
                    logDownload(file); // Registrar la descarga
                } else {
                    Toast.makeText(getContext(), R.string.download_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteFile(FileMetadata file) {
                // Los trabajadores no tienen permiso para borrar archivos.
            }
        };

        adapter = new FileAdapter(manualList, fileActionListener, false); // false -> no mostrar botón de borrado
        recyclerView.setAdapter(adapter);
        loadManuals();

        return view;
    }

    private void loadManuals() {
        DatabaseReference filesRef = FirebaseDatabase.getInstance("https://capacitacion-material-default-rtdb.firebaseio.com/").getReference("files");
        filesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                manualList.clear();
                for (DataSnapshot fileSnapshot : snapshot.getChildren()) {
                    FileMetadata metadata = fileSnapshot.getValue(FileMetadata.class);
                    if (metadata != null) {
                        metadata.setFileId(fileSnapshot.getKey());
                        manualList.add(metadata);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar los manuales desde Firebase", error.toException());
                Toast.makeText(getContext(), "Error al cargar los manuales.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logDownload(FileMetadata file) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || file == null || file.getFileId() == null) return;

        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "Email Desconocido";

        HistoricoArchivo historico = new HistoricoArchivo(file.getFileId(), file.getFileName(), userId, userEmail);

        databaseManager.registrarDescargaArchivo(historico, new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Registro de descarga guardado exitosamente.");
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Fallo al guardar el registro de descarga: " + message);
            }
        });

        if (file.getFileName() != null && file.getFileName().toLowerCase().endsWith(".pdf")) {
            sendAuditNotification(userEmail, file.getFileName());
        }
    }

    private void sendAuditNotification(String downloaderName, String fileName) {
        if (getContext() == null) return;

        String auditUrl = "https://capacitacion-mrodante.netlify.app/.netlify/functions/send-audit-notification";
        JSONObject postData = new JSONObject();
        try {
            postData.put("userId", downloaderName);
            postData.put("action", "Descarga de Manual");
            postData.put("details", "Archivo: " + fileName);
        } catch (JSONException e) {
            Log.e("AuditError", "Error al crear el JSON de auditoría", e);
            return;
        }

        JsonObjectRequest auditRequest = new JsonObjectRequest(Request.Method.POST, auditUrl, postData,
                response -> Log.d("AuditSuccess", "Notificación de auditoría enviada."),
                error -> Log.e("AuditError", "Fallo al enviar notificación de auditoría.", error)
        );

        Volley.newRequestQueue(getContext()).add(auditRequest);
    }
}
