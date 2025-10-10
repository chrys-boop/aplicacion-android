package metro.plascreem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG ="LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager();

        etEmail = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Por favor, ingrese correo y contraseña.", Toast.LENGTH_SHORT).show();
                return;
            }

            databaseManager.loginUser(email, password, new DatabaseManager.AuthListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();

                            // ✅ IMPORTANTE: Suscribir al usuario al topic \"all\" para recibir notificaciones
                            subscribeToNotifications();

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        fetchUserDataAndRedirect(user.getUid());
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: no se pudo obtener el usuario después del inicio de sesión.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(LoginActivity.this, "Error en el inicio de sesión: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Toast.makeText(LoginActivity.this, "Sesión ya iniciada. Redirigiendo...", Toast.LENGTH_SHORT).show();
                    // También suscribir si ya hay sesión activa
                    subscribeToNotifications();
            fetchUserDataAndRedirect(currentUser.getUid());
        }
    }

    private void fetchUserDataAndRedirect(String userId) {
        databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
            @Override
            public void onDataReceived(Map<String, Object> userData) {
                if (userData == null || !(userData.get("userType") instanceof String)) {
                        Toast.makeText(LoginActivity.this, "No se pudo determinar el rol del usuario. El rol no está definido o es inválido.", Toast.LENGTH_LONG).show();
                return;
            }

            String userType = ((String) userData.get("userType")).trim();

                    Intent intent;
                switch (userType) {
                case "Administrador":
                    intent = new Intent(LoginActivity.this, Administrador.class);
                    break;
                case "Enlaces":
                    intent = new Intent(LoginActivity.this, Enlaces.class);
                    break;
                case "Personal Administrativo":
                    intent = new Intent(LoginActivity.this, Personal_Administrativo.class);
                    break;
                case "Trabajadores":
                    intent = new Intent(LoginActivity.this, Trabajadores.class);
                    String nombre = (String) userData.get("nombreCompleto");
                            String expediente = (String) userData.get("numeroExpediente");
                                    intent.putExtra("NOMBRE_COMPLETO", nombre);
                                            intent.putExtra("NUMERO_EXPEDIENTE", expediente);
                    break;
                default:
                    Toast.makeText(LoginActivity.this,"Rol de usuario no reconocido: [" + userType + "]", Toast.LENGTH_LONG).show();
                    return;
            }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        @Override
        public void onDataCancelled(String message) {
            Toast.makeText(LoginActivity.this, "Error al obtener datos de usuario: " + message, Toast.LENGTH_LONG).show();
        }
    });
}

/**
 * 🔔 Suscribe al usuario al topic \"all\" para recibir notificaciones push
 * cuando se creen nuevos eventos desde cualquier administrador
 */
private void subscribeToNotifications() {
    Log.d(TAG, "🔔 Iniciando suscripción a notificaciones...");

            // Solicitar permiso de notificaciones en Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "📱 Solicitando permiso de notificaciones Android 13+...");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        } else {
            Log.d(TAG, "✅ Permiso de notificaciones ya otorgado");
        }
    }

    // Obtener el token de FCM para debugging
    FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(tokenTask -> {
                if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                    String token = tokenTask.getResult();
                    Log.d(TAG, "🔑 FCM Token obtenido: " + token.substring(0, Math.min(20, token.length())) + "...");
                } else {
                    Log.e(TAG, "❌ Error al obtener FCM token", tokenTask.getException());
                }
            });

    // Suscribir al topic \"all\"
    FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "✅✅✅ Usuario suscrito EXITOSAMENTE al topic 'all' ✅✅✅");
                            Toast.makeText(LoginActivity.this, "Notificaciones activadas ✓", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "❌❌❌ ERROR al suscribir al topic 'all' ❌❌❌");
                    if (task.getException() != null) {
                        Log.e(TAG, "Detalle del error:", task.getException());
                                task.getException().printStackTrace();
                    }
                    Toast.makeText(LoginActivity.this, "Error al activar notificaciones", Toast.LENGTH_SHORT).show();
                }
            });
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 1) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "✅ Permiso de notificaciones OTORGADO por el usuario");
                    Toast.makeText(this, "Notificaciones habilitadas", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "❌ Permiso de notificaciones DENEGADO por el usuario");
                    Toast.makeText(this, "Las notificaciones están deshabilitadas. Puedes activarlas en Configuración.", Toast.LENGTH_LONG).show();
        }
    }
}
}
