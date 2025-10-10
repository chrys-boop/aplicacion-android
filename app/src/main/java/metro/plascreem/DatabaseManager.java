package metro.plascreem;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private final StorageReference mStorageRef;
    private static final String TAG = "DatabaseManager";


    public DatabaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://capacitacion-material-default-rtdb.firebaseio.com/").getReference();

        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    // --- Interfaces para callbacks ---

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

    public void getFilesUploadedByUser(String userId, FileHistoryListener listener) {
        mDatabase.child("files").orderByChild("uploaderId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FileMetadata> fileList = new ArrayList<>();
                for (DataSnapshot fileSnapshot : snapshot.getChildren()) {
                    FileMetadata metadata = fileSnapshot.getValue(FileMetadata.class);
                    if (metadata != null) {
                        metadata.setFileId(fileSnapshot.getKey()); // Guardar el ID del archivo
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

    public void updateUserProfile(String userId, String fullName, String email, String expediente, String taller, String enlaceOrigen, String horario, DataSaveListener listener) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("nombreCompleto", fullName);
        updatedData.put("email", email);
        updatedData.put("numeroExpediente", expediente);
        updatedData.put("taller", taller);
        updatedData.put("enlaceOrigen", enlaceOrigen);
        updatedData.put("horario", horario);

        mDatabase.child("users").child(userId).updateChildren(updatedData)
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
        }

        if (eventId != null) {
            mDatabase.child("events").child(eventId).setValue(event)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        } else {
            listener.onFailure("No se pudo generar un ID para el evento.");
        }
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
}
