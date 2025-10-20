
package metro.plascreem;

// Importante para que Firebase pueda ignorar campos nulos al guardar
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String uid;
    private String nombreCompleto;
    private String numeroExpediente;
    private String email;
    private String userType;
    private long lastConnection;
    private String fcmToken; // <-- AÑADIDO

    // --- NUEVOS CAMPOS ---
    private String area;
    private String titular;

    // Otros campos que podrían ser útiles
    private String taller;
    private String enlaceOrigen;
    private String horario;


    // Constructor vacío requerido para Firebase
    public User() {}

    // --- GETTERS Y SETTERS --- //

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNumeroExpediente() {
        return numeroExpediente;
    }

    public void setNumeroExpediente(String numeroExpediente) {
        this.numeroExpediente = numeroExpediente;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public long getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(long lastConnection) {
        this.lastConnection = lastConnection;
    }

    // --- GETTER Y SETTER PARA FCM TOKEN ---
    public String getFcmToken() { // <-- AÑADIDO
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) { // <-- AÑADIDO
        this.fcmToken = fcmToken;
    }

    // --- GETTERS Y SETTERS PARA NUEVOS CAMPOS ---

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getTaller() {
        return taller;
    }

    public void setTaller(String taller) {
        this.taller = taller;
    }

    public String getEnlaceOrigen() {
        return enlaceOrigen;
    }

    public void setEnlaceOrigen(String enlaceOrigen) {
        this.enlaceOrigen = enlaceOrigen;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    // Esto es para que el Spinner muestre el nombre del usuario
    @Override
    public String toString() {
        return nombreCompleto != null ? nombreCompleto : "Usuario sin nombre";
    }
}
