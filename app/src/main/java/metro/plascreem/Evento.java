package metro.plascreem;

import android.os.Parcel;
import android.os.Parcelable;

public class Evento implements Parcelable {
    // Constantes: Etiquetas para clasificar el evento
    public static final String TIPO_DOCUMENTO = "DOCUMENTO";
    public static final String TIPO_MEDIA = "MEDIA";
    public static final String TIPO_SIMPLE = "SIMPLE";

    private String id;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String tipoAccion;

    // Constructor vacío requerido para Firebase
    public Evento() {}

    public Evento(String id, String titulo, String descripcion, String fecha, String tipoAccion) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.tipoAccion = tipoAccion;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getTipoAccion() { return tipoAccion; }
    public String getFecha() { return fecha; }

    // --- Setter ---
    public void setId(String id) {
        this.id = id;
    }

    // --- Implementación de Parcelable ---

    protected Evento(Parcel in) {
        id = in.readString();
        titulo = in.readString();
        descripcion = in.readString();
        fecha = in.readString();
        tipoAccion = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(titulo);
        dest.writeString(descripcion);
        dest.writeString(fecha);
        dest.writeString(tipoAccion);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Evento> CREATOR = new Creator<Evento>() {
        @Override
        public Evento createFromParcel(Parcel in) {
            return new Evento(in);
        }

        @Override
        public Evento[] newArray(int size) {
            return new Evento[size];
        }
    };
}
