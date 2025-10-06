package metro.plascreem;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Splashcreem extends AppCompatActivity {

    // Define la duración de la pantalla de bienvenida en milisegundos
    private static final int SPLASH_TIME_OUT = 3000; // 3 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashcreem); // Asegúrate de tener este archivo de layout

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Inicia la actividad principal de tu app
                Intent intent = new Intent(Splashcreem.this,LoginActivity.class);
                startActivity(intent);

                // Cierra esta actividad para que el usuario no pueda regresar a ella
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}