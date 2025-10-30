package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
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

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    // *** INICIO: SOLUCIÓN DE NOTIFICACIONES ***
    private String senderIdFromNotification;
    // *** FIN: SOLUCIÓN DE NOTIFICACIONES ***

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(this);

        // *** INICIO: SOLUCIÓN DE NOTIFICACIONES ***
        // Procesa el intent inicial para capturar el senderId si la app se abre desde una notificación.
        handleIntent(getIntent());
        // *** FIN: SOLUCIÓN DE NOTIFICACIONES ***

        etEmail = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        if (getIntent().hasExtra("REGISTRATION_SUCCESS")) {
            Toast.makeText(this, getIntent().getStringExtra("REGISTRATION_SUCCESS"), Toast.LENGTH_LONG).show();
        }

        btnLogin.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Botón de login presionado", Toast.LENGTH_SHORT).show();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                ToastUtils.showShortToast(this, "Por favor, ingrese correo y contraseña.");
                return;
            }

            databaseManager.loginUser(email, password, new DatabaseManager.AuthListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "databaseManager.loginUser onSuccess: Autenticación exitosa.");
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        loginSuccessSequence(user.getUid());
                    } else {
                        Log.e(TAG, "onSuccess: mAuth.getCurrentUser() retornó null después de un inicio de sesión exitoso.");
                        ToastUtils.showLongToast(LoginActivity.this, "Error crítico: no se pudo obtener el usuario después del inicio de sesión.");
                    }
                }

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, "databaseManager.loginUser onFailure: " + message);
                    ToastUtils.showLongToast(LoginActivity.this, "Error en el inicio de sesión: " + message);
                }
            });
        });

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // *** INICIO: SOLUCIÓN DE NOTIFICACIONES ***
        // Procesa el intent si la app ya estaba abierta y recibe una notificación.
        handleIntent(intent);
        // *** FIN: SOLUCIÓN DE NOTIFICACIONES ***

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loginSuccessSequence(currentUser.getUid());
        }
    }

    // *** INICIO: SOLUCIÓN DE NOTIFICACIONES ***
    // Método centralizado para extraer y guardar de forma segura el senderId del Intent.
    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("senderId")) {
            this.senderIdFromNotification = intent.getStringExtra("senderId");
            intent.removeExtra("senderId"); // Se limpia para evitar reprocesamiento.
        }
    }
    // *** FIN: SOLUCIÓN DE NOTIFICACIONES ***

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            ToastUtils.showShortToast(this, "Por favor, ingrese su correo electrónico para restablecer la contraseña.");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ToastUtils.showLongToast(this, "Se ha enviado un correo para restablecer su contraseña.");
                    } else {
                        ToastUtils.showLongToast(this, "Error al enviar el correo de restablecimiento.");
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loginSuccessSequence(currentUser.getUid());
        }
    }

    private void loginSuccessSequence(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tokenTask -> {
            String fcmToken = (tokenTask.isSuccessful() && tokenTask.getResult() != null) ? tokenTask.getResult() : "";

            databaseManager.updateUserFcmToken(userId, fcmToken, new DatabaseManager.DataSaveListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Token de FCM actualizado.");
                    fetchUserDataAndRedirect(userId);
                }

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, "Error al actualizar token, aun así se procede: " + message);
                    fetchUserDataAndRedirect(userId);
                }
            });
        });
    }

    private void fetchUserDataAndRedirect(String userId) {
        databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
            @Override
            public void onDataReceived(Map<String, Object> userData) {
                if (userData == null) {
                    Log.e(TAG, "fetchUserDataAndRedirect: No se pudieron obtener los datos del usuario desde la base de datos (userData es null).");
                    ToastUtils.showLongToast(LoginActivity.this, "No se pudieron obtener los datos del usuario.");
                    mAuth.signOut();
                    return;
                }

                Object policyAcceptedObj = userData.get("policyAccepted");
                boolean policyAccepted = policyAcceptedObj instanceof Boolean && (Boolean) policyAcceptedObj;

                if (!policyAccepted) {
                    Log.w(TAG, "fetchUserDataAndRedirect: El usuario no ha aceptado la política de privacidad. Redirigiendo a PrivacyAcceptanceActivity.");
                    ToastUtils.showLongToast(LoginActivity.this, "Debe aceptar la política de privacidad para continuar.");
                    Intent intent = new Intent(LoginActivity.this, PrivacyAcceptanceActivity.class);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                    finishAffinity();
                    return;
                }

                // *** INICIO: SOLUCIÓN DE NOTIFICACIONES ***
                // Si se guardó un senderId, se redirige a ChatActivity y se detiene el flujo.
                if (senderIdFromNotification != null && !senderIdFromNotification.isEmpty()) {
                    Log.d(TAG, "Redirigiendo a ChatActivity desde notificación. Remitente: " + senderIdFromNotification);

                    Intent chatIntent = new Intent(LoginActivity.this, ChatActivity.class);
                    chatIntent.putExtra("senderId", senderIdFromNotification);
                    senderIdFromNotification = null; // Se limpia la variable.

                    startActivity(chatIntent);
                    finish();
                    return; // IMPORTANTE: Detiene la ejecución para no redirigir al perfil.
                }
                // *** FIN: SOLUCIÓN DE NOTIFICACIONES ***

                Object userTypeObj = userData.get("userType");
                if (!(userTypeObj instanceof String) || ((String) userTypeObj).trim().isEmpty()) {
                    Log.e(TAG, "fetchUserDataAndRedirect: El rol del usuario ('userType') es nulo, vacío o no es un String. Valor: " + userTypeObj);
                    ToastUtils.showLongToast(LoginActivity.this, "No se pudo determinar el rol del usuario. Contacte a soporte.");
                    mAuth.signOut();
                    return;
                }
                String userType = ((String) userTypeObj).trim();
                Log.d(TAG, "Rol de usuario obtenido: " + userType);

                if (userType.equalsIgnoreCase("instructor")) userType = "Instructor";
                else if (userType.equalsIgnoreCase("administrador")) userType = "Administrador";
                else if (userType.equalsIgnoreCase("personal administrativo")) userType = "Personal Administrativo";
                else if (userType.equalsIgnoreCase("enlaces")) userType = "Enlaces";
                else if (userType.equalsIgnoreCase("trabajadores")) userType = "Trabajadores";

                subscribeToNotifications();

                String nombre = (String) userData.get("nombreCompleto");
                String expediente = (String) userData.get("numeroExpediente");

                Intent intent;

                // *** INICIO: CÓDIGO RESTAURADO ***
                // Se restaura la lógica de suscripción a tópicos específicos por rol.
                switch (userType) {
                    case "Administrador":
                        intent = new Intent(LoginActivity.this, Administrador.class);
                        FirebaseMessaging.getInstance().subscribeToTopic("admins").addOnCompleteListener(task -> Log.d(TAG, "Subscribed to 'admins' topic."));
                        break;
                    case "Personal Administrativo":
                        intent = new Intent(LoginActivity.this, Personal_Administrativo.class);
                        FirebaseMessaging.getInstance().subscribeToTopic("admins").addOnCompleteListener(task -> Log.d(TAG, "Subscribed to 'admins' topic."));
                        break;
                    case "Enlaces":
                        intent = new Intent(LoginActivity.this, Enlaces.class);
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("admins").addOnCompleteListener(task -> Log.d(TAG, "Unsubscribed from 'admins' topic."));
                        break;
                    case "Trabajadores":
                        intent = new Intent(LoginActivity.this, Trabajadores.class);
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("admins").addOnCompleteListener(task -> Log.d(TAG, "Unsubscribed from 'admins' topic."));
                        break;
                    case "Instructor":
                        intent = new Intent(LoginActivity.this, Instructores.class);
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("admins").addOnCompleteListener(task -> Log.d(TAG, "Unsubscribed from 'admins' topic."));
                        break;
                    default:
                        Log.e(TAG, "fetchUserDataAndRedirect: Rol de usuario no reconocido: [" + userType + "]");
                        Toast.makeText(LoginActivity.this, "Rol de usuario no reconocido: [" + userType + "]", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                }
                // *** FIN: CÓDIGO RESTAURADO ***

                Log.d(TAG, "Redirigiendo al usuario a la actividad: " + intent.getComponent().getClassName());
                if (nombre != null) intent.putExtra("NOMBRE_COMPLETO", nombre);
                if (expediente != null) intent.putExtra("NUMERO_EXPEDIENTE", expediente);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onDataCancelled(String message) {
                Log.e(TAG, "fetchUserDataAndRedirect onDataCancelled: " + message);
                ToastUtils.showLongToast(LoginActivity.this, "Error al obtener datos de usuario: " + message);
                mAuth.signOut();
            }
        });
    }

    private void subscribeToNotifications() {
        Log.d(TAG, "🔔 Iniciando suscripción a notificaciones...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "📱 Solicitando permiso de notificaciones Android 13+...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                Log.d(TAG, "✅ Permiso de notificaciones ya otorgado");
            }
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(tokenTask -> {
                    if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                        String token = tokenTask.getResult();
                        Log.d(TAG, "🔑 FCM Token obtenido: " + token.substring(0, Math.min(20, token.length())) + "...");
                    } else {
                        Log.e(TAG, "❌ Error al obtener FCM token", tokenTask.getException());
                    }
                });

        FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅✅✅ Usuario suscrito EXITOSAMENTE al topic 'all' ✅✅✅");
                    } else {
                        Log.e(TAG, "❌❌❌ ERROR al suscribir al topic 'all' ❌❌❌");
                        if (task.getException() != null) {
                            Log.e(TAG, "Detalle del error:", task.getException());
                        }
                        ToastUtils.showShortToast(LoginActivity.this, "Error al activar notificaciones");
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Permiso de notificaciones OTORGADO por el usuario");
                ToastUtils.showShortToast(this, "Notificaciones habilitadas");
            } else {
                Log.e(TAG, "❌ Permiso de notificaciones DENEGADO por el usuario");
                ToastUtils.showLongToast(this, "Las notificaciones están deshabilitadas. Puedes activarlas en Configuración.");
            }
        }
    }
}





