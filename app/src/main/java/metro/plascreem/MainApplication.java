package metro.plascreem;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class MainApplication extends Application {

    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String THEME_KEY = "theme_pref";

    @Override
    public void onCreate() {
        super.onCreate();

        // --- INICIALIZACIÓN TEMPRANA DE SUPABASE EN EL HILO PRINCIPAL ---
        // Esto es crucial para evitar el error "addObserver must be called on the main thread"
        SupabaseManager.init();

        // Carga la preferencia del tema guardada
        SharedPreferences settingsPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Obtiene el modo guardado. Por defecto, seguirá la configuración del sistema.
        int nightMode = settingsPrefs.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Aplica el tema a toda la aplicación
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}