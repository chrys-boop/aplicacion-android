
package metro.plascreem;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private final StorageReference mStorageRef;
    private static final String TAG = "DatabaseManager";
    private final RequestQueue requestQueue;
    private final Context context;


    public DatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }

    // --- Interfaces para callbacks ---
    public interface DataCallback<T> {
        void onDataReceived(T data);
        void onDataCancelled(String message);
    }

    public interface AuthListener {
        void onSuccess();
        void onFailure(String message);
    }

    public interface UserDataMapListener {
        void onDataReceived(Map<String, Object> userData);
        void onDataCancelled(String message);
    }

    public interface UploadListener {
        void onSuccess(String downloadUrl);
        void onFailure(String message);
        void onProgress(double progress);
    }

    public interface DataSaveListener {
        void onSuccess();
        void onFailure(String message);
    }

    public interface DatabaseListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface EventsListener {
        void onEventsReceived(List<Evento> events);
        void onCancelled(String message);
    }

    public interface AllUsersListener {
        void onUsersReceived(List<User> users);
        void onCancelled(String message);
    }

    public interface FileHistoryListener {
        void onHistoryReceived(List<FileMetadata> history);
        void onCancelled(String message);
    }


    // --- Métodos de autenticación y base de datos ---

    public void registerUser(String email, String password, String fullName, String expediente, String userType, AuthListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onSuccess();
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("userType", userType);
                        userData.put("nombreCompleto", fullName);
                        userData.put("numeroExpediente", expediente);
                        userData.put("lastConnection", System.currentTimeMillis());

                        mDatabase.child("users").child(userId).setValue(userData)
                                .addOnCompleteListener(dbTask -> {
                                    if (!dbTask.isSuccessful()) {
                                        Log.e(TAG, "Error al guardar datos de usuario.", dbTask.getException());
                                    }
                                });
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }


    public void loginUser(String email, String password, AuthListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        mDatabase.child("users").child(userId).child("lastConnection").setValue(System.currentTimeMillis());
                        listener.onSuccess();
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void getUsersByRole(String role, final DataCallback<List<User>> listener) {
        mDatabase.child("users").orderByChild("userType").equalTo(role).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> userList = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(userSnapshot.getKey());
                        userList.add(user);
                    }
                }
                listener.onDataReceived(userList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onDataCancelled(error.getMessage());
            }
        });
    }

    public void getAllUsers(AllUsersListener listener) {
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> userList = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(userSnapshot.getKey());
                        userList.add(user);
                    }
                }
                listener.onUsersReceived(userList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCancelled(error.getMessage());
            }
        });
    }
    // --- NUEVO MÉTODO PARA OBTENER TODOS LOS USUARIOS (con DataCallback) ---
    public void getAllUsers(final DataCallback<List<User>> callback) {
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> userList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        userList.add(user);
                    }
                }
                callback.onDataReceived(userList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onDataCancelled(databaseError.getMessage());
            }
        });
    }

    public void getFilesUploadedByUser(String userId, FileHistoryListener listener) {
        mDatabase.child("files").orderByChild("uploaderId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FileMetadata> fileList = new ArrayList<>();
                for (DataSnapshot fileSnapshot : snapshot.getChildren()) {
                    FileMetadata metadata = fileSnapshot.getValue(FileMetadata.class);
                    if (metadata != null) {
                        metadata.setFileId(fileSnapshot.getKey());
                        fileList.add(metadata);
                    }
                }
                listener.onHistoryReceived(fileList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCancelled(error.getMessage());
            }
        });
    }


    public void getUserDataMap(String userId, final UserDataMapListener listener) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userData = (Map<String, Object>) snapshot.getValue();
                    listener.onDataReceived(userData);
                } else {
                    listener.onDataCancelled("No data available for this user.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onDataCancelled(error.getMessage());
            }
        });
    }

    public void uploadFile(Uri fileUri, String fileName, String uploaderId, String uploaderName, UploadListener listener) {
        String storagePath = "documents/" + uploaderId + "/" + fileName;
        StorageReference fileRef = mStorageRef.child(storagePath);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    long sizeBytes = taskSnapshot.getMetadata() != null ? taskSnapshot.getMetadata().getSizeBytes() : -1;

                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveFileMetadata(fileName, downloadUri.toString(), storagePath, sizeBytes, uploaderId, new DataSaveListener() {
                            @Override
                            public void onSuccess() {
                                triggerAuditNotification("upload", fileName, uploaderName);
                                listener.onSuccess(downloadUri.toString());
                            }

                            @Override
                            public void onFailure(String message) {
                                listener.onFailure(message);
                            }
                        });
                    }).addOnFailureListener(e -> {
                        listener.onFailure(e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    listener.onFailure(e.getMessage());
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    listener.onProgress(progress);
                });
    }


    public void saveFileMetadata(String fileName, String downloadUrl, String storagePath, long size, String uploaderId, DataSaveListener listener) {
        String fileId = mDatabase.child("files").push().getKey();
        FileMetadata fileData = new FileMetadata(fileId, fileName, downloadUrl, storagePath, uploaderId, size, System.currentTimeMillis());

        if (fileId != null) {
            mDatabase.child("files").child(fileId).setValue(fileData)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        } else {
            listener.onFailure("Could not generate file ID.");
        }
    }

    public void updateUserFcmToken(String userId, String token, @NonNull final DataSaveListener listener) {
        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) {
            Log.w(TAG, "No se puede actualizar el token de FCM: userId o token son nulos/vacíos.");
            listener.onFailure("UserID o Token inválido.");
            return;
        }

        Log.d(TAG, "Actualizando token de FCM para el usuario: " + userId);
        mDatabase.child("users").child(userId).child("fcmToken").setValue(token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Token de FCM actualizado exitosamente.");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al actualizar el token de FCM.", e);
                    listener.onFailure(e.getMessage());
                });
    }

    public void logDownloadEvent(String userId, String userName, String fileName, @NonNull final DataSaveListener listener) {
        if (userId == null || userId.isEmpty() || fileName == null || fileName.isEmpty()) {
            listener.onFailure("UserID o FileName inválido.");
            return;
        }

        String downloadId = mDatabase.child("downloads").push().getKey();
        if (downloadId == null) {
            listener.onFailure("No se pudo generar un ID para el evento de descarga.");
            return;
        }

        Map<String, Object> downloadData = new HashMap<>();
        downloadData.put("downloaderId", userId);
        downloadData.put("fileName", fileName);
        downloadData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("downloads").child(downloadId).setValue(downloadData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Evento de descarga registrado para el usuario: " + userId);
                    triggerAuditNotification("download", fileName, userName);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al registrar el evento de descarga.", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // --- NUEVO MÉTODO PARA REGISTRAR DESCARGAS CON REALTIME DATABASE ---
    public void registrarDescargaArchivo(HistoricoArchivo historico, DataSaveListener listener) {
        String key = mDatabase.child("file_download_history").push().getKey();
        if (key == null) {
            listener.onFailure("No se pudo generar una clave para el registro de descarga.");
            return;
        }

        mDatabase.child("file_download_history").child(key).setValue(historico)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Registro de descarga guardado con clave: " + key);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar registro de descarga", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // --- MÉTODO ACTUALIZADO --- //
    public void updateUserProfile(String userId, String fullName, String email, String expediente, String taller, String enlaceOrigen, String horario, String area, String titular, DataSaveListener listener) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("nombreCompleto", fullName);
        updatedData.put("email", email);
        updatedData.put("numeroExpediente", expediente);
        updatedData.put("taller", taller);
        updatedData.put("enlaceOrigen", enlaceOrigen);
        updatedData.put("horario", horario);
        updatedData.put("area", area); // Añadido
        updatedData.put("titular", titular); // Añadido

        mDatabase.child("users").child(userId).updateChildren(updatedData)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // --- NUEVO MÉTODO PARA EL PERFIL DEL TRABAJADOR ---
    public void updateWorkerProfile(String userId, Map<String, Object> workerProfile, DataSaveListener listener) {
        mDatabase.child("users").child(userId).updateChildren(workerProfile)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getEventsForDate(String date, EventsListener listener) {
        mDatabase.child("events").orderByChild("fecha").equalTo(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Evento> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Evento event = eventSnapshot.getValue(Evento.class);
                    if (event != null) {
                        event.setId(eventSnapshot.getKey());
                        events.add(event);
                    }
                }
                listener.onEventsReceived(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCancelled(error.getMessage());
            }
        });
    }

    public void getAllEvents(EventsListener listener) {
        mDatabase.child("events").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Evento> events = new ArrayList<>();
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    Evento event = eventSnapshot.getValue(Evento.class);
                    if (event != null) {
                        event.setId(eventSnapshot.getKey());
                        events.add(event);
                    }
                }
                listener.onEventsReceived(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCancelled(error.getMessage());
            }
        });
    }

    public void saveEvent(Evento event, DataSaveListener listener) {
        String eventId = event.getId();
        if (eventId == null || eventId.isEmpty()) {
            eventId = mDatabase.child("events").push().getKey();
            event.setId(eventId);
        }

        if (eventId != null) {
            mDatabase.child("events").child(eventId).setValue(event)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        } else {
            listener.onFailure("No se pudo generar un ID para el evento.");
        }
    }

    public void deleteEvent(String eventId, DataSaveListener listener) {
        if (eventId == null || eventId.isEmpty()) {
            listener.onFailure("ID de evento inválido.");
            return;
        }
        mDatabase.child("events").child(eventId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Evento eliminado con éxito: " + eventId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar el evento: " + eventId, e);
                    listener.onFailure(e.getMessage());
                });
    }

    public void deleteFileRecord(String fileId, final DatabaseListener listener) {
        if (fileId == null || fileId.isEmpty()) {
            listener.onFailure("ID de archivo inválido.");
            return;
        }
        mDatabase.child("files").child(fileId).removeValue()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    private void triggerAuditNotification(String eventType, String fileName, String user) {
        String url = "https://ubiquitous-pithivier-c55883.netlify.app/.netlify/functions/send-audit-notification";

        JSONObject postData = new JSONObject();
        try {
            postData.put("eventType", eventType);
            postData.put("fileName", fileName);
            postData.put("user", user);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for audit notification", e);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                postData,
                response -> Log.i(TAG, "Audit notification sent successfully."),
                error -> Log.e(TAG, "Error sending audit notification: " + error.toString())
        );

        requestQueue.add(jsonObjectRequest);
    }
}
