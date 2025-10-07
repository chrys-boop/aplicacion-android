package metro.plascreem;

import android.net.Uri;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private final StorageReference mStorageRef;


    public DatabaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
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

    public interface EventsListener {
        void onEventsReceived(List<Evento> events);
        void onCancelled(String message);
    }

    // --- Métodos de autenticación y base de datos ---

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

                        mDatabase.child("users").child(userId).setValue(userData)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        listener.onSuccess();
                                    } else {
                                        listener.onFailure(dbTask.getException().getMessage());
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
                        listener.onSuccess();
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void getUserDataMap(String userId, final UserDataMapListener listener) {
        mDatabase.child("users").child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();
                    if (snapshot.exists()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> userData = (Map<String, Object>) snapshot.getValue();
                        listener.onDataReceived(userData);
                    } else {
                        listener.onDataCancelled("No data available for this user.");
                    }
                } else {
                    listener.onDataCancelled(task.getException().getMessage());
                }
            }
        });
    }

    public void uploadFile(Uri fileUri, String fileName, UploadListener listener) {
        StorageReference fileRef = mStorageRef.child("documents/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        listener.onSuccess(downloadUri.toString());
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

    public void uploadMediaFile(Uri fileUri, String fileName, UploadListener listener) {
        StorageReference fileRef = mStorageRef.child("media/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        listener.onSuccess(downloadUri.toString());
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

    public void saveFileMetadata(String fileName, String downloadUrl, long size, String uploaderId, DataSaveListener listener) {
        String fileId = mDatabase.child("files").push().getKey();
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileName", fileName);
        fileData.put("downloadUrl", downloadUrl);
        fileData.put("timestamp", System.currentTimeMillis());
        fileData.put("size", size);
        fileData.put("uploaderId", uploaderId);

        if (fileId != null) {
            mDatabase.child("files").child(fileId).setValue(fileData)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        }
    }

    public void updateUserProfile(String userId, String fullName, String email, DataSaveListener listener) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("nombreCompleto", fullName);
        updatedData.put("email", email);

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
}
