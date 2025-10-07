package metro.plascreem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etFullName, etExpediente;
    private Spinner spinnerRole;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private DatabaseManager databaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_register_activity);

        databaseManager = new DatabaseManager();

        // Inicializar vistas
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etFullName = findViewById(R.id.et_register_fullname);
        etExpediente = findViewById(R.id.et_register_expediente);
        spinnerRole = findViewById(R.id.spinner_role);
        btnRegister = findViewById(R.id.btn_register_user);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        // Configurar el Spinner de roles
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.user_roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Listener para el botón de registrar
        btnRegister.setOnClickListener(v -> registerUser());

        // Listener para ir a la pantalla de login
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String expediente = etExpediente.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        // Validaciones
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(fullName) || TextUtils.isEmpty(expediente)) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (role.equals("Seleccione un rol")) {
            Toast.makeText(this, "Por favor, seleccione un rol válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Llamar al método de registro (que actualizaremos a continuación)
        databaseManager.registerUser(email, password, fullName, expediente, role, new DatabaseManager.AuthListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(RegisterActivity.this, "Registro exitoso. Por favor inicie sesión.", Toast.LENGTH_LONG).show();
                // Redirigir al login después de un registro exitoso
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(RegisterActivity.this, "Error en el registro: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
