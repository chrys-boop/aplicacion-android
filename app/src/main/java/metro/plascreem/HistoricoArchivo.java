package metro.plascreem;

// Clase de modelo para los archivos en el histórico
public class HistoricoArchivo {
    private String nombreArchivo;
    private String extension; // Ejemplo: PDF, JPG, MP4
    private String usuario;
    private String horaSubida;

    // Constructor
    public HistoricoArchivo(String nombreArchivo, String extension, String usuario, String horaSubida) {
        this.nombreArchivo = nombreArchivo;
        this.extension = extension;
        this.usuario = usuario;
        this.horaSubida = horaSubida;
    }

    // Getters
    public String getNombreArchivo() { return nombreArchivo; }
    public String getExtension() { return extension; }
    public String getUsuario() { return usuario; }
    public String getHoraSubida() { return horaSubida; }
}