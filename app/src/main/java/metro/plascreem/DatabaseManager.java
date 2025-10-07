package metro.plascreem;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;

    public DatabaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
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


    // --- Métodos de autenticación y base de datos ---

    /**
     * @deprecated Usar el nuevo método registerUser que incluye nombre completo y expediente.
     */
    @Deprecated
    public void registerUser(String email, String password, String userType, AuthListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userType", userType);
                        userData.put("email", email);

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

    // NUEVO MÉTODO DE REGISTRO
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
                                        // En caso de fallo en la BD, se podría considerar eliminar el usuario recién creado para consistencia
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
}

