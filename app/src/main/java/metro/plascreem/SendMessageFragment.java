package metro.plascreem;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SendMessageFragment extends Fragment {

    private static final String TAG = "SendMessageFragment";

    private AutoCompleteTextView roleSpinner, userSpinner;
    private EditText messageEditText;
    private Button sendMessageButton, sendAlertButton;
    private ProgressBar progressBar;

    private DatabaseManager dbManager;
    private List<User> userList = new ArrayList<>();
    private ArrayAdapter<User> userAdapter;
    private User selectedUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_send_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbManager = new DatabaseManager(getContext());

        roleSpinner = view.findViewById(R.id.spinner_user_role);
        userSpinner = view.findViewById(R.id.spinner_select_user);
        messageEditText = view.findViewById(R.id.et_message_content);
        sendMessageButton = view.findViewById(R.id.btn_send_message);
        sendAlertButton = view.findViewById(R.id.btn_send_alert);
        progressBar = view.findViewById(R.id.progress_bar_send);

        setupRoleSpinner();
        setupUserSpinner();
        setupButtons();
    }

    private void setupRoleSpinner() {
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.user_roles, android.R.layout.simple_dropdown_item_1line);
        roleSpinner.setAdapter(roleAdapter);

        roleSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position > 0) {
                setTargetedMessagingControlsEnabled(true);
                loadUsersByRole(parent.getItemAtPosition(position).toString());
            } else {
                setTargetedMessagingControlsEnabled(false);
            }
        });
    }

    private void setupUserSpinner() {
        userAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, userList);
        userSpinner.setAdapter(userAdapter);

        userSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedUser = (User) parent.getItemAtPosition(position);
        });
    }

    private void setupButtons() {
        sendMessageButton.setOnClickListener(v -> handleSendMessage());
        sendAlertButton.setOnClickListener(v -> handleSendAlert());
        setTargetedMessagingControlsEnabled(false);
    }

    // *** INICIO DE LA CORRECCIÓN ***
    private void handleSendMessage() {
        if (selectedUser == null || selectedUser.getUid() == null) {
            Toast.makeText(getContext(), "Por favor, selecciona un usuario específico.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String messageContent = messageEditText.getText().toString().trim();
        if (messageContent.isEmpty()) {
            Toast.makeText(getContext(), "El mensaje no puede estar vacío.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: no se pudo identificar al remitente.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String currentUserId = currentUser.getUid();
        final String recipientId = selectedUser.getUid();
        final String recipientToken = selectedUser.getFcmToken();

        if (recipientToken == null || recipientToken.isEmpty()) {
            Toast.makeText(getContext(), "Este usuario no puede recibir notificaciones en este momento.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // 1. Guardar el mensaje en la base de datos usando la lógica centralizada.
        dbManager.sendDirectMessage(currentUserId, recipientId, messageContent, new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                // 2. Si se guarda con éxito, obtener el nombre del remitente para la notificación.
                dbManager.getUserDataMap(currentUserId, new DatabaseManager.UserDataMapListener() {
                    @Override
                    public void onDataReceived(Map<String, Object> userData) {
                        String senderName = (userData != null && userData.get("nombreCompleto") != null) ? (String) userData.get("nombreCompleto") : "Alguien";
                        String notificationTitle = "Nuevo mensaje de " + senderName;
                        // 3. Enviar la notificación PUSH.
                        sendPushNotification(notificationTitle, messageContent, recipientToken, null, currentUserId);
                    }

                    @Override
                    public void onDataCancelled(String message) {
                        // Si falla obtener el nombre, enviar notificación con título genérico.
                        sendPushNotification("Nuevo Mensaje", messageContent, recipientToken, null, currentUserId);
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al enviar el mensaje: " + message, Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }
    // *** FIN DE LA CORRECIÓN ***

    private void handleSendAlert() {
        String messageContent = messageEditText.getText().toString().trim();
        if (messageContent.isEmpty()) {
            Toast.makeText(getContext(), "El mensaje de alerta no puede estar vacío.", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        // Las alertas generales no necesitan un senderId porque no abren un chat específico.
        sendPushNotification("¡Alerta Importante!", messageContent, null, "all", null);
    }

    private void loadUsersByRole(String role) {
        setLoading(true);
        dbManager.getUsersByRole(role, new DatabaseManager.DataCallback<List<User>>() {
            @Override
            public void onDataReceived(List<User> data) {
                userList.clear();
                userList.add(new User("Seleccionar Usuario")); // Placeholder
                userList.addAll(data);
                userAdapter.notifyDataSetChanged();
                userSpinner.setText("", false);
                selectedUser = null;
                setLoading(false);
            }
            @Override
            public void onDataCancelled(String message) {
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void sendPushNotification(String title, String body, @Nullable String token, @Nullable String topic, @Nullable String senderId) {
        String functionUrl = "https://capacitacion-mrodante.netlify.app/.netlify/functions/send-notification";
        JSONObject payload = new JSONObject();
        try {
            payload.put("title", title);
            payload.put("body", body);
            // Especifica que es una notificación de chat
            payload.put("isChatMessage", true);

            if (token != null) payload.put("token", token);
            if (topic != null) payload.put("topic", topic);
            if (senderId != null) payload.put("senderId", senderId);

        } catch (JSONException e) {
            Log.e(TAG, "Error creando JSON para notificación", e);
            setLoading(false);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, functionUrl, payload,
                response -> {
                    Toast.makeText(getContext(), "Mensaje enviado.", Toast.LENGTH_SHORT).show();
                    messageEditText.setText("");
                    setLoading(false);
                    if (getActivity() != null) getParentFragmentManager().popBackStack();
                },
                error -> {
                    Log.e(TAG, "Error al enviar la notificación: " + error.toString());
                    Toast.makeText(getContext(), "Error al enviar la notificación.", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                }
        );
        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        sendMessageButton.setEnabled(!isLoading);
        sendAlertButton.setEnabled(!isLoading);
        roleSpinner.setEnabled(!isLoading);
        userSpinner.setEnabled(!isLoading);
    }

    private void setTargetedMessagingControlsEnabled(boolean isEnabled) {
        userSpinner.setEnabled(isEnabled);
        sendMessageButton.setEnabled(isEnabled);
        if (!isEnabled) {
            userList.clear();
            userList.add(new User("Seleccione un rol primero"));
            userAdapter.notifyDataSetChanged();
            userSpinner.setText("", false);
            selectedUser = null;
        }
    }
}
