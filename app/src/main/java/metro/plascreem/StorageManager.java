
package metro.plascreem;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class StorageManager {

    private static final String TAG = "StorageManager";
    private final FirebaseStorage storage;

    // --- INTERFACES DE LISTENER --- //

    public interface StorageListener {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface FileListListener {
        void onSuccess(List<StorageReference> files);
        void onFailure(String error);
    }

    public interface DownloadUrlListener {
        void onSuccess(String url);
        void onFailure(String error);
    }

    // --- CONSTRUCTOR --- //

    public StorageManager() {
        storage = FirebaseStorage.getInstance();
    }

    // --- NUEVO MÉTODO PARA LISTAR ARCHIVOS ---
    public void listFilesInFolder(String folderPath, final FileListListener listener) {
        StorageReference listRef = storage.getReference().child(folderPath);

        listRef.listAll()
                .addOnSuccessListener(listResult -> {
                    Log.d(TAG, "Archivos listados correctamente en: " + folderPath);
                    listener.onSuccess(listResult.getItems());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error al listar archivos en: " + folderPath, exception);
                    listener.onFailure(exception.getMessage());
                });
    }

    // --- NUEVO MÉTODO PARA OBTENER URL DE DESCARGA ---
    public void getDownloadUrl(String fullPath, final DownloadUrlListener listener) {
        if (fullPath == null || fullPath.isEmpty()) {
            listener.onFailure("La ruta del archivo es inválida.");
            return;
        }

        StorageReference fileRef = storage.getReference().child(fullPath);

        fileRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    listener.onSuccess(uri.toString());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error al obtener la URL de descarga para: " + fullPath, exception);
                    listener.onFailure(exception.getMessage());
                });
    }

    // --- MÉTODO EXISTENTE PARA BORRAR ARCHIVOS ---
    public void deleteFile(String storageUrl, final StorageListener listener) {
        if (storageUrl == null || storageUrl.isEmpty()) {
            listener.onFailure("URL de archivo inválida.");
            return;
        }

        // El path en la base de datos es la URL completa, no la ruta.
        // Necesitamos obtener la referencia desde la URL.
        StorageReference fileRef = storage.getReferenceFromUrl(storageUrl);

        fileRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Archivo eliminado exitosamente de Storage: " + storageUrl);
                    listener.onSuccess("Archivo eliminado del almacenamiento.");
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error al eliminar de Storage: " + storageUrl, exception);
                    listener.onFailure("Error al eliminar de Storage: " + exception.getMessage());
                });
    }
}
