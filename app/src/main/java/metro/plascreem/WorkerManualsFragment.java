
package metro.plascreem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// AUDITORÍA: Imports necesarios para la llamada a la función de Netlify
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

        OnFileActionListener fileActionListener = new OnFileActionListener() {
            @Override
            public void onViewFile(FileMetadata file) {
                if (file != null && file.getDownloadUrl() != null && !file.getDownloadUrl().isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getDownloadUrl()));
                    startActivity(browserIntent);

                    logDownload(file);
                } else {
                    Log.e(TAG, "La URL de descarga es nula o está vacía.");
                    ToastUtils.showShortToast(getActivity(), "No se puede abrir el archivo.");
                }
            }

            @Override
            public void onDeleteFile(FileMetadata file) {
                // Los trabajadores no pueden borrar archivos. Dejar vacío.
            }
        };

        adapter = new FileAdapter(manualList, fileActionListener, false);
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
                        manualList.add(metadata);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al cargar los manuales desde Firebase", error.toException());
                if (getActivity() != null) {
                    ToastUtils.showLongToast(getActivity(), "Error al cargar los manuales.");
                }
            }
        });
    }

    private void logDownload(FileMetadata file) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No se puede registrar la descarga porque el usuario no ha iniciado sesión.");
            return;
        }

        String userId = currentUser.getUid();
        String downloaderName = currentUser.getDisplayName();
        if (downloaderName == null || downloaderName.isEmpty()) {
            downloaderName = currentUser.getEmail();
            if (downloaderName == null || downloaderName.isEmpty()) {
                downloaderName = "Usuario Desconocido";
            }
        }

        // AUDITORÍA: Envía notificación si el archivo es un PDF.
        if (file.getFileName() != null && file.getFileName().toLowerCase().endsWith(".pdf")) {
            sendAuditNotification(downloaderName, file.getFileName());
        }

        Log.d(TAG, "Registrando descarga del manual '" + file.getFileName() + "' por el usuario: " + userId);

        databaseManager.logDownloadEvent(userId, downloaderName, file.getFileName(), new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Registro de descarga exitoso para auditoría.");
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Fallo al registrar la descarga para auditoría: " + message);
            }
        });
    }

    // AUDITORÍA: Nuevo método para enviar la notificación de auditoría de descarga.
    private void sendAuditNotification(String downloaderName, String fileName) {
        if (getContext() == null) {
            Log.e("AuditError", "El contexto es nulo, no se puede enviar la notificación de auditoría.");
            return;
        }

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
                response -> Log.d("AuditSuccess", "Notificación de auditoría enviada por descarga de PDF: " + fileName),
                error -> Log.e("AuditError", "Fallo al enviar la notificación de auditoría por descarga", error)
        );

        // Añadir la petición a la cola de Volley.
        Volley.newRequestQueue(getContext()).add(auditRequest);
    }
}
