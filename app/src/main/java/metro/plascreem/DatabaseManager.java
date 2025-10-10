package metro.plascreem;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;
    private final StorageReference mStorageRef;
    private static final String TAG = "DatabaseManager";


    public DatabaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
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


    public void registerUser(String email, String password, String fullName, String expediente, String userType, AuthListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("userType", userType);
                        userData.put("nombreCompleto", fullName);
                        userData.put("numeroExpediente", expediente);
                        userData.put("lastConnection", System.currentTimeMillis());

                        mFirestore.collection("users").document(userId).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Usuario registrado exitosamente en Firestore");
                                    listener.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error al guardar datos de usuario en Firestore", e);
                                    listener.onFailure(e.getMessage());
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
                        mFirestore.collection("users").document(userId)
                                .update("lastConnection", System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error actualizando lastConnection", e);
                                    listener.onSuccess();
                                });
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void getAllUsers(AllUsersListener listener) {
        mFirestore.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> userList = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUid(document.getId());
                            userList.add(user);
                        }
                    }
                    listener.onUsersReceived(userList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error obteniendo usuarios", e);
                    listener.onCancelled(e.getMessage());
                });
    }

    public void getFilesUploadedByUser(String userId, FileHistoryListener listener) {
        mFirestore.collection("files")
                .whereEqualTo("uploaderId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FileMetadata> fileList = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        FileMetadata metadata = document.toObject(FileMetadata.class);
                        if (metadata != null) {
                            metadata.setFileId(document.getId());
                            fileList.add(metadata);
                        }
                    }
                    listener.onHistoryReceived(fileList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error obteniendo archivos del usuario", e);
                    listener.onCancelled(e.getMessage());
                });
    }


    public void getUserDataMap(String userId, final UserDataMapListener listener) {
        mFirestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> userData = documentSnapshot.getData();
                        if (userData != null) {
                            listener.onDataReceived(userData);
                        } else {
                            listener.onDataCancelled("No data available for this user.");
                        }
                    } else {
                        listener.onDataCancelled("User document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error obteniendo datos del usuario", e);
                    listener.onDataCancelled(e.getMessage());
                });
    }

    public void uploadFile(Uri fileUri, String fileName, String uploaderId, UploadListener listener) {
        String storagePath = "documents/" + uploaderId + "/" + fileName;
        StorageReference fileRef = mStorageRef.child(storagePath);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    long sizeBytes = taskSnapshot.getMetadata() != null ? taskSnapshot.getMetadata().getSizeBytes() : -1;

                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveFileMetadata(fileName, downloadUri.toString(), storagePath, sizeBytes, uploaderId, new DataSaveListener() {
                            @Override
                            public void onSuccess() {
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
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileName", fileName);
        fileData.put("downloadUrl", downloadUrl);
        fileData.put("storagePath", storagePath);
        fileData.put("uploaderId", uploaderId);
        fileData.put("size", size);
        fileData.put("timestamp", System.currentTimeMillis());

        mFirestore.collection("files").add(fileData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Archivo guardado con ID: " + documentReference.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando metadata del archivo", e);
                    listener.onFailure(e.getMessage());
                });
    }

    public void updateUserProfile(String userId, String fullName, String email, String expediente, String taller, String enlaceOrigen, String horario, DataSaveListener listener) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("nombreCompleto", fullName);
        updatedData.put("email", email);
        updatedData.put("numeroExpediente", expediente);
        updatedData.put("taller", taller);
        updatedData.put("enlaceOrigen", enlaceOrigen);
        updatedData.put("horario", horario);

        mFirestore.collection("users").document(userId).update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Perfil actualizado exitosamente");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error actualizando perfil", e);
                    listener.onFailure(e.getMessage());
                });
    }

    public void getEventsForDate(String date, EventsListener listener) {
        mFirestore.collection("events")
                .whereEqualTo("fecha", date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Evento> events = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Evento event = document.toObject(Evento.class);
                        if (event != null) {
                            event.setId(document.getId());
                            events.add(event);
                        }
                    }
                    listener.onEventsReceived(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error obteniendo eventos", e);
                    listener.onCancelled(e.getMessage());
                });
    }

    public void saveEvent(Evento event, DataSaveListener listener) {
        String eventId = event.getId();

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("fecha", event.getFecha());
        eventData.put("hora", event.getHora());
        eventData.put("titulo", event.getTitulo());
        eventData.put("descripcion", event.getDescripcion());

        if (eventId == null || eventId.isEmpty()) {
            mFirestore.collection("events").add(eventData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Evento creado con ID: " + documentReference.getId());
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando evento", e);
                        listener.onFailure(e.getMessage());
                    });
        } else {
            mFirestore.collection("events").document(eventId).set(eventData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Evento actualizado");
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error actualizando evento", e);
                        listener.onFailure(e.getMessage());
                    });
        }
    }

    public void deleteFileRecord(String fileId, final DatabaseListener listener) {
        if (fileId == null || fileId.isEmpty()) {
            listener.onFailure("ID de archivo inválido.");
            return;
        }
        mFirestore.collection("files").document(fileId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Registro de archivo eliminado");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error eliminando registro de archivo", e);
                    listener.onFailure(e.getMessage());
                });
    }
}
