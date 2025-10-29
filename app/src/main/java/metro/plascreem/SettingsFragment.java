package metro.plascreem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String REMINDERS_KEY = "event_reminders_enabled";
    private static final String THEME_KEY = "theme_pref";

    private SharedPreferences settingsPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupReminderSwitch(view);
        setupThemeSelector(view);

        view.findViewById(R.id.tv_contact_support).setOnClickListener(v -> contactSupport());

        // Lógica para Política de Privacidad, compatible con todos los roles.
        view.findViewById(R.id.tv_privacy_policy).setOnClickListener(v -> {
            FragmentActivity activity = getActivity();
            if (activity == null) return;

            int containerId = -1;
            // IDs verificados a partir del código fuente de cada Activity
            if (activity instanceof Administrador) {
                containerId = R.id.admin_fragment_container;
            } else if (activity instanceof Enlaces) {
                containerId = R.id.fragment_container;
            } else if (activity instanceof Trabajadores) {
                containerId = R.id.main_fragment_container;
            } else if (activity instanceof Instructores) {
                containerId = R.id.fragment_container_instructores;
            } else if (activity instanceof Personal_Administrativo) {
                containerId = R.id.admin_fragment_container; // Confirmado que usa el mismo que Administrador
            }

            if (containerId != -1) {
                getParentFragmentManager().beginTransaction()
                        .replace(containerId, new PrivacyPolicyFragment()) // Se usa el mismo Fragment para todos
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Error: No se pudo determinar la pantalla actual.", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.tv_terms_conditions).setOnClickListener(v -> openUrl("https://www.plascreem.com/terminos-condiciones"));
    }

    private void setupThemeSelector(View view) {
        view.findViewById(R.id.tv_theme_selector).setOnClickListener(v -> showThemeDialog());
    }

    private void showThemeDialog() {
        String[] themes = {"Claro", "Oscuro", "Automático (del sistema)"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Seleccionar Tema");
        builder.setItems(themes, (dialog, which) -> {
            SharedPreferences.Editor editor = settingsPrefs.edit();
            switch (which) {
                case 0: // Claro
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor.putInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case 1: // Oscuro
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor.putInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case 2: // Automático
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    editor.putInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }
            editor.apply();
        });
        builder.create().show();
    }

    private void setupReminderSwitch(View view) {
        SwitchMaterial switchReminders = view.findViewById(R.id.switch_event_reminders);
        boolean remindersEnabled = settingsPrefs.getBoolean(REMINDERS_KEY, true);
        switchReminders.setChecked(remindersEnabled);

        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean(REMINDERS_KEY, isChecked).apply();
            String notificationTopic = "all";

            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic(notificationTopic)
                        .addOnCompleteListener(task -> {
                            String msg = "Recordatorios activados";
                            if (!task.isSuccessful()) {
                                msg = "Error al activar recordatorios";
                                Log.w(TAG, "Suscripción al topic 'all' fallida", task.getException());
                            }
                            showToast(msg);
                        });
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(notificationTopic)
                        .addOnCompleteListener(task -> {
                            String msg = "Recordatorios desactivados";
                            if (!task.isSuccessful()) {
                                msg = "Error al desactivar recordatorios";
                                Log.w(TAG, "Desuscripción del topic 'all' fallida", task.getException());
                            }
                            showToast(msg);
                        });
            }
        });
    }

    private void contactSupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:soporte@plascreem.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Soporte App Plas-Creem");

        if (requireActivity().getPackageManager() != null && intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            showToast("No se encontró una aplicación de correo.");
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));

        if (requireActivity().getPackageManager() != null && intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            showToast("No se encontró un navegador web.");
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}


