package metro.plascreem;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class StorageManager {

    private final FirebaseStorage storage;

    public interface StorageListener {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public StorageManager() {
        storage = FirebaseStorage.getInstance();
    }

    public void deleteFile(String storagePath, final StorageListener listener) {
        if (storagePath == null || storagePath.isEmpty()) {
            listener.onFailure("Ruta de archivo inválida.");
            return;
        }

        StorageReference fileRef = storage.getReferenceFromUrl(storagePath);

        fileRef.delete().addOnSuccessListener(aVoid -> {
            listener.onSuccess("Archivo eliminado del almacenamiento.");
        }).addOnFailureListener(exception -> {
            listener.onFailure("Error al eliminar de Storage: " + exception.getMessage());
        });
    }
}
