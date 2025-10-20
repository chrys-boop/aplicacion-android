package metro.plascreem;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendMessageFragment extends Fragment {

    private Spinner roleSpinner, userSpinner;
    private EditText messageEditText;
    private Button sendMessageButton, sendAlertButton;
    private ProgressBar progressBar;

    private DatabaseManager dbManager;
    private List<User> userList = new ArrayList<>();
    private ArrayAdapter<User> userAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_send_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbManager = new DatabaseManager(getContext());

        // Inicializar Vistas
        roleSpinner = view.findViewById(R.id.spinner_user_role);
        userSpinner = view.findViewById(R.id.spinner_select_user);
        messageEditText = view.findViewById(R.id.et_message_content);
        sendMessageButton = view.findViewById(R.id.btn_send_message);
        sendAlertButton = view.findViewById(R.id.btn_send_alert);
        progressBar = view.findViewById(R.id.progress_bar_send);

        // Configurar Spinner de Roles
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.user_roles, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        // Configurar Spinner de Usuarios
        userAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, userList);
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userSpinner.setAdapter(userAdapter);

        // Listener para el Spinner de Roles
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = parent.getItemAtPosition(position).toString();
                if (!selectedRole.equals("Seleccionar Rol")) {
                    loadUsersByRole(selectedRole);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Listeners para los botones
        sendMessageButton.setOnClickListener(v -> sendMessage("message"));
        sendAlertButton.setOnClickListener(v -> sendMessage("alert"));
    }

    private void loadUsersByRole(String role) {
        progressBar.setVisibility(View.VISIBLE);
        dbManager.getUsersByRole(role, new DatabaseManager.DataCallback<List<User>>() {
            @Override
            public void onDataReceived(List<User> data) {
                userList.clear();
                userList.addAll(data);
                userAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onDataCancelled(String message) {
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void sendMessage(String type) {
        User selectedUser = (User) userSpinner.getSelectedItem();
        String messageContent = messageEditText.getText().toString().trim();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (selectedUser == null) {
            Toast.makeText(getContext(), "Por favor, selecciona un usuario.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (messageContent.isEmpty()) {
            Toast.makeText(getContext(), "El mensaje no puede estar vacío.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("direct_messages").child(selectedUser.getUid());
        String messageId = messagesRef.push().getKey();
        long timestamp = System.currentTimeMillis();

        DirectMessage directMessage = new DirectMessage(messageId, currentUserId, selectedUser.getUid(), messageContent, type, timestamp);

        messagesRef.child(messageId).setValue(directMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Mensaje guardado en DB, ahora enviamos la notificación push
                sendPushNotificationToUser(selectedUser.getFcmToken(), type, messageContent);
            } else {
                Toast.makeText(getContext(), "Error al guardar el mensaje.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void sendPushNotificationToUser(String userToken, String type, String message) {
        String title = type.equals("alert") ? "¡Alerta Importante!" : "Nuevo Mensaje";

        // URL de la función Netlify
        String functionUrl = "https://capacitacion-mrodante.netlify.app/.netlify/functions/send-notification";

        JSONObject payload = new JSONObject();
        try {
            payload.put("token", userToken);
            payload.put("title", title);
            payload.put("body", message);
        } catch (JSONException e) {
            Log.e("SendMessageFragment", "Error creando JSON para notificación", e);
            progressBar.setVisibility(View.GONE);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, functionUrl, payload,
                response -> {
                    Toast.makeText(getContext(), "Notificación enviada con éxito", Toast.LENGTH_SHORT).show();
                    messageEditText.setText("");
                    progressBar.setVisibility(View.GONE);
                    getParentFragmentManager().popBackStack(); // Regresar
                },
                error -> {
                    Log.e("NotificationError", "Error: " + error.toString());
                    Toast.makeText(getContext(), "Error al enviar la notificación push", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
        );

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        queue.add(request);
    }
}
