package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class Splashcreem extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000; // 3 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashcreem);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Prepara el Intent para ir a LoginActivity
                Intent loginIntent = new Intent(Splashcreem.this, LoginActivity.class);

                // *** INICIO: PASE DE DATOS REFORZADO ***
                // Se busca explícitamente el 'senderId' del Intent original (de la notificación)
                // y se pasa de forma segura al siguiente Intent.
                if (getIntent().hasExtra("senderId")) {
                    String senderId = getIntent().getStringExtra("senderId");
                    if (senderId != null && !senderId.isEmpty()) {
                        Log.d("Splashcreem", "Pasando senderId a LoginActivity: " + senderId);
                        loginIntent.putExtra("senderId", senderId);
                    }
                }
                // *** FIN: PASE DE DATOS REFORZADO ***

                startActivity(loginIntent);
                finish(); // Cierra esta actividad
            }
        }, SPLASH_TIME_OUT);
    }
}