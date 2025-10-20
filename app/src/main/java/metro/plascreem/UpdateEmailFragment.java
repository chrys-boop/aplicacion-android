package metro.plascreem;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UpdateEmailFragment extends Fragment {

    private static final String TAG = "UpdateEmailFragment";

    private EditText etNewEmail, etCurrentPassword;
    private Button btnUpdateEmail;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_update_email, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        etNewEmail = view.findViewById(R.id.et_new_email);
        etCurrentPassword = view.findViewById(R.id.et_current_password_for_email);
        btnUpdateEmail = view.findViewById(R.id.btn_save_password);

        btnUpdateEmail.setOnClickListener(v -> updateEmail());
    }

    private void updateEmail() {
        String newEmail = etNewEmail.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newEmail) || TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(getContext(), "Por favor, introduce un correo electrónico válido", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(getContext(), "No se pudo encontrar el usuario. Inicie sesión nuevamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-autenticar al usuario por seguridad
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Si la re-autenticación es exitosa, actualizar el correo
                user.updateEmail(newEmail).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(getContext(), "Correo electrónico actualizado exitosamente", Toast.LENGTH_SHORT).show();
                        // Cerrar el fragmento y volver a la pantalla anterior
                        getParentFragmentManager().popBackStack();
                    } else {
                        Log.e(TAG, "Error al actualizar el correo", updateTask.getException());
                        Toast.makeText(getContext(), "Error al actualizar el correo: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Log.e(TAG, "Error de re-autenticación", task.getException());
                Toast.makeText(getContext(), "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

