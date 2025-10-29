package metro.plascreem;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyAcceptanceActivity extends AppCompatActivity {

    private DatabaseManager databaseManager;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_acceptance);

        databaseManager = new DatabaseManager(this);

        TextView tvPolicy = findViewById(R.id.text_privacy_policy);
        Button btnAccept = findViewById(R.id.btn_accept);

        if (tvPolicy == null || btnAccept == null) {
            Toast.makeText(this, "Error crítico: La interfaz de aceptación no se pudo cargar.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvPolicy.setMovementMethod(new ScrollingMovementMethod());

        userId = getIntent().getStringExtra("USER_ID");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error crítico: No se ha podido identificar al usuario.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnAccept.setOnClickListener(v -> {
            btnAccept.setEnabled(false);
            acceptPolicy();
        });
    }

    private void acceptPolicy() {
        databaseManager.setUserPolicyAcceptanceByUid(userId, true, new DatabaseManager.AuthListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(PrivacyAcceptanceActivity.this, "Política aceptada. Redirigiendo...", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(PrivacyAcceptanceActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PrivacyAcceptanceActivity.this, "Error al guardar: " + message, Toast.LENGTH_LONG).show();
                    Button btnAccept = findViewById(R.id.btn_accept);
                    if(btnAccept != null) {
                        btnAccept.setEnabled(true);
                    }
                });
            }
        });
    }
}


