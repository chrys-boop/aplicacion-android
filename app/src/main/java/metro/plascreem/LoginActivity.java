package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister; // Nuevo TextView
    private DatabaseManager databaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseManager = new DatabaseManager();

        etEmail = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register); // Inicializar TextView

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
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    fetchUserDataAndRedirect(userId);
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(LoginActivity.this, "Error en el inicio de sesión: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        // Listener para el nuevo TextView
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void fetchUserDataAndRedirect(String userId) {
        databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
            @Override
            public void onDataReceived(Map<String, Object> userData) {
                if (userData == null || !userData.containsKey("userType")) {
                    Toast.makeText(LoginActivity.this, "No se pudo determinar el rol del usuario.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userType = (String) userData.get("userType");

                Intent intent;
                switch (userType) {
                    case "Administrador":
                        intent = new Intent(LoginActivity.this, Administrador.class);
                        break;
                    case "Enlaces":
                        intent = new Intent(LoginActivity.this, Enlaces.class);
                        break;
                    case "Personal":
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
                        Toast.makeText(LoginActivity.this, "Rol de usuario no reconocido.", Toast.LENGTH_SHORT).show();
                        return;
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onDataCancelled(String message) {
                Toast.makeText(LoginActivity.this, "Error al obtener datos de usuario: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}