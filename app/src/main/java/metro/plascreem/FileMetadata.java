package metro.plascreem;

public class FileMetadata {
    private String name;
    private String url;
    private String path; // Ruta en Firebase Storage

    // Constructor vacío requerido para Firebase
    public FileMetadata() {}

    public FileMetadata(String name, String url, String path) {
        this.name = name;
        this.url = url;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}