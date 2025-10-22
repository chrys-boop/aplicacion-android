package metro.plascreem;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String uid;
    private String nombreCompleto;
    private String numeroExpediente;
    private String email;
    private String userType;
    private Long lastConnection;
    private String fcmToken;

    private String area;
    private String titular;
    private String taller;
    private String enlaceOrigen;
    private String horario;

    // Constructor vacío requerido para Firebase
    public User() {}

    // --- NUEVO CONSTRUCTOR AÑADIDO ---
    // Usado para crear usuarios "placeholder" en los Spinners.
    public User(String placeholderName) {
        this.nombreCompleto = placeholderName;
    }

    // --- GETTERS Y SETTERS --- //

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getNumeroExpediente() { return numeroExpediente; }
    public void setNumeroExpediente(String numeroExpediente) { this.numeroExpediente = numeroExpediente; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public Long getLastConnection() { return lastConnection; }
    public void setLastConnection(Long lastConnection) { this.lastConnection = lastConnection; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public String getTaller() { return taller; }
    public void setTaller(String taller) { this.taller = taller; }

    public String getEnlaceOrigen() { return enlaceOrigen; }
    public void setEnlaceOrigen(String enlaceOrigen) { this.enlaceOrigen = enlaceOrigen; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }

    @Override
    public String toString() {
        return nombreCompleto != null ? nombreCompleto : "Usuario sin nombre";
    }
}
