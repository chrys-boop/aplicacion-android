package metro.plascreem;

// Importa las clases necesarias
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;
import androidx.core.content.ContextCompat;

public class LoginActivity extends AppCompatActivity {

    private EditText etExpediente, etPassword;
    private Button btnLogin, btnEnlaces, btnPersonal, btnTrabajadores, btnAdministrador;
    private String selectedRole = "Enlaces";

    // Simulación de la base de datos de usuarios
    private Map<String, String> userCredentials = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Llenar la "base de datos" simulada
        userCredentials.put("12345_12345", "Enlaces");
        userCredentials.put("67890_67890", "Personal");
        userCredentials.put("54321_54321", "Trabajadores");
        userCredentials.put("09876_09876", "Administrador");

        // Inicializar vistas
        etExpediente = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnEnlaces = findViewById(R.id.btn_enlaces);
        btnPersonal = findViewById(R.id.btn_personal);
        btnTrabajadores = findViewById(R.id.btn_trabajadores);
        btnAdministrador = findViewById(R.id.btn_administrador);

        // Configurar el listener para los botones de rol
        btnEnlaces.setOnClickListener(v -> selectRole("Enlaces"));
        btnPersonal.setOnClickListener(v -> selectRole("Personal"));
        btnTrabajadores.setOnClickListener(v -> selectRole("Trabajadores"));
        btnAdministrador.setOnClickListener(v -> selectRole("Administrador"));

        // Llama a selectRole() para configurar el color inicial al abrir la app
        selectRole(selectedRole);

        // Configurar el listener para el botón de inicio de sesión
        btnLogin.setOnClickListener(v -> {
            String expediente = etExpediente.getText().toString();
            String password = etPassword.getText().toString();

            String userKey = expediente + "_" + password;

            if (userCredentials.containsKey(userKey) && userCredentials.get(userKey).equals(selectedRole)) {
                Toast.makeText(this, "Inicio de sesión exitoso como " + selectedRole, Toast.LENGTH_SHORT).show();

                // Lógica de navegación basada en el rol
                if (selectedRole.equals("Administrador")) {
                    Intent intent = new Intent(this,Administrador.class);
                    intent.putExtra("NOMBRE_COMPLETO", "Nombre Admin");
                    intent.putExtra("NUMERO_EXPEDIENTE", expediente);
                    startActivity(intent);
                } else if (selectedRole.equals("Enlaces")) {
                    Intent intent = new Intent(this, Enlaces.class);
                    intent.putExtra("NOMBRE_COMPLETO", "Nombre Enlace");
                    intent.putExtra("NUMERO_EXPEDIENTE", expediente);
                    startActivity(intent);
                } else if (selectedRole.equals("Personal")) {
                    Intent intent = new Intent(this, Personal_Administrativo.class);
                    intent.putExtra("NOMBRE_COMPLETO", "Nombre Personal");
                    intent.putExtra("NUMERO_EXPEDIENTE", expediente);
                    startActivity(intent);
                } else if (selectedRole.equals("Trabajadores")) {
                    Intent intent = new Intent(this, Trabajadores.class);
                    intent.putExtra("NOMBRE_COMPLETO", "Nombre Trabajador");
                    intent.putExtra("NUMERO_EXPEDIENTE", expediente);
                    startActivity(intent);
                }
                finish();

            } else {
                Toast.makeText(this, "Credenciales incorrectas o rol no coincidente.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectRole(String role) {
        selectedRole = role;

        // Reinicia el color de todos los botones
        btnEnlaces.setBackgroundColor(ContextCompat.getColor(this, R.color.colorInactivo));
        btnPersonal.setBackgroundColor(ContextCompat.getColor(this, R.color.colorInactivo));
        btnTrabajadores.setBackgroundColor(ContextCompat.getColor(this, R.color.colorInactivo));
        btnAdministrador.setBackgroundColor(ContextCompat.getColor(this, R.color.colorInactivo));

        // Cambia el color del botón seleccionado a "activo"
        switch (role) {
            case "Enlaces":
                btnEnlaces.setBackgroundColor(ContextCompat.getColor(this, R.color.colorActivo));
                break;
            case "Personal":
                btnPersonal.setBackgroundColor(ContextCompat.getColor(this, R.color.colorActivo));
                break;
            case "Trabajadores":
                btnTrabajadores.setBackgroundColor(ContextCompat.getColor(this, R.color.colorActivo));
                break;
            case "Administrador":
                btnAdministrador.setBackgroundColor(ContextCompat.getColor(this, R.color.colorActivo));
                break;
        }
    }
}