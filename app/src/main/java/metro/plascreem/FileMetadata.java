package metro.plascreem;

import com.google.firebase.database.Exclude;

// Modelo de datos para la metadata de cada archivo subido.
public class FileMetadata {

    private String fileId;       // ID único del archivo en la Realtime Database
    private String fileName;     // Nombre original del archivo (ej: "reporte.pdf")
    private String downloadUrl;  // URL pública para descargar el archivo desde Storage
    private String storagePath;  // Ruta completa del archivo en Firebase Storage
    private String uploaderId;   // UID del usuario que subió el archivo
    private long size;           // Tamaño del archivo en bytes
    private long timestamp;      // Marca de tiempo de la subida (en milisegundos)

    // Constructor vacío es requerido por Firebase para deserializar los datos
    public FileMetadata() {}

    // Constructor completo para facilitar la creación de nuevos objetos
    public FileMetadata(String fileId, String fileName, String downloadUrl, String storagePath, String uploaderId, long size, long timestamp) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.storagePath = storagePath;
        this.uploaderId = uploaderId;
        this.size = size;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    // Marcamos el fileId con @Exclude para que Firebase no intente guardarlo dos veces (como clave y como campo)
    @Exclude
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getStoragePath() { return storagePath; }
    public String getUploaderId() { return uploaderId; }
    public long getSize() { return size; }
    public long getTimestamp() { return timestamp; }

    // --- Setters ---
    public void setFileId(String fileId) { this.fileId = fileId; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public void setUploaderId(String uploaderId) { this.uploaderId = uploaderId; }
    public void setSize(long size) { this.size = size; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Métodos obsoletos para compatibilidad (pueden ser eliminados después de una migración)
    @Exclude
    private String name;
    @Exclude
    private String url;
    @Exclude
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    @Exclude
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
