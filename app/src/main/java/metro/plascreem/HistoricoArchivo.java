package metro.plascreem;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

/**
 * Modelo de datos para registrar un evento de descarga en Realtime Database.
 */
public class HistoricoArchivo {

    private String fileId;
    private String fileName;
    private String userId;
    private String userEmail;
    private Object timestamp; // Se usa Object para poder manejar el ServerValue.TIMESTAMP

    // Constructor vacío requerido para Firebase
    public HistoricoArchivo() {}

    public HistoricoArchivo(String fileId, String fileName, String userId, String userEmail) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.userId = userId;
        this.userEmail = userEmail;
        // Al escribir, Firebase reemplazará esto con la hora del servidor
        this.timestamp = ServerValue.TIMESTAMP;
    }

    // --- Getters ---
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }

    // Getter para el timestamp. Firebase lo usará al escribir.
    public Object getTimestamp() { return timestamp; }

    // Getter auxiliar para leer el timestamp como un Long una vez que está en la BD.
    @Exclude
    public Long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        }
        return null;
    }

    // --- Setters ---
    public void setFileId(String fileId) { this.fileId = fileId; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }
}