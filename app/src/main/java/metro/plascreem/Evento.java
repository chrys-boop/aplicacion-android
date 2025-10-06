package metro.plascreem;

public class Evento {
    // Constantes: Etiquetas para clasificar el evento
    public static final String TIPO_DOCUMENTO = "DOCUMENTO";
    public static final String TIPO_MEDIA = "MEDIA";
    public static final String TIPO_SIMPLE = "SIMPLE";

    private String id;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String tipoAccion;

    public Evento(String id, String titulo, String descripcion, String fecha, String tipoAccion) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.tipoAccion = tipoAccion;
    }

    // Getters necesarios para el adaptador y la navegación
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getTipoAccion() { return tipoAccion; }
    public String getFecha() { return fecha; }
}

