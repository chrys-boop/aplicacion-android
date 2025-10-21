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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG ="LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(this);

        etEmail = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                ToastUtils.showShortToast(this, "Por favor, ingrese correo y contraseña.");
                return;
            }

            databaseManager.loginUser(email, password, new DatabaseManager.AuthListener() {
                @Override
                public void onSuccess() {
                    ToastUtils.showShortToast(LoginActivity.this, "Inicio de sesión exitoso.");
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        updateFcmTokenAndRedirect(user.getUid());
                    } else {
                        ToastUtils.showLongToast(LoginActivity.this, "Error: no se pudo obtener el usuario después del inicio de sesión.");
                    }
                }

                @Override
                public void onFailure(String message) {
                    ToastUtils.showLongToast(LoginActivity.this, "Error en el inicio de sesión: " + message);
                }
            });
        });

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
    }

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
                        Log.d(TAG, "Email de restablecimiento enviado a: " + email);
                    } else {
                        ToastUtils.showLongToast(this, "Error al enviar el correo de restablecimiento.");
                        Log.e(TAG, "Error en sendPasswordResetEmail", task.getException());
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            ToastUtils.showShortToast(this, "Sesión ya iniciada. Redirigiendo...");
            updateFcmTokenAndRedirect(currentUser.getUid());
        }
    }

    private void updateFcmTokenAndRedirect(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String fcmToken = task.getResult();
                databaseManager.updateUserFcmToken(userId, fcmToken, new DatabaseManager.DataSaveListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Token de FCM actualizado exitosamente para el usuario: " + userId);
                        fetchUserDataAndRedirect(userId);
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.e(TAG, "Error al actualizar el token de FCM: " + message);
                        fetchUserDataAndRedirect(userId);
                    }
                });
            } else {
                Log.e(TAG, "No se pudo obtener el token de FCM.", task.getException());
                fetchUserDataAndRedirect(userId);
            }
        });
    }

    private void fetchUserDataAndRedirect(String userId) {
        databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
            @Override
            public void onDataReceived(Map<String, Object> userData) {
                if (userData == null || !(userData.get("userType") instanceof String)) {
                    ToastUtils.showLongToast(LoginActivity.this, "No se pudo determinar el rol del usuario. El rol no está definido o es inválido.");
                    return;
                }

                subscribeToNotifications();

                String userType = ((String) userData.get("userType")).trim();
                String nombre = (String) userData.get("nombreCompleto");
                String expediente = (String) userData.get("numeroExpediente");

                Intent intent;

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
                    default:
                        Toast.makeText(LoginActivity.this, "Rol de usuario no reconocido: [" + userType + "]", Toast.LENGTH_LONG).show();
                        return;
                }

                if (nombre != null) {
                    intent.putExtra("NOMBRE_COMPLETO", nombre);
                }
                if (expediente != null) {
                    intent.putExtra("NUMERO_EXPEDIENTE", expediente);
                }

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onDataCancelled(String message) {
                ToastUtils.showLongToast(LoginActivity.this, "Error al obtener datos de usuario: " + message);
            }
        });
    }

    private void subscribeToNotifications() {
        Log.d(TAG, "🔔 Iniciando suscripción a notificaciones...");

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
                            task.getException().printStackTrace();
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
