package metro.plascreem;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private Button buttonSend;

    private MessageAdapter messageAdapter;
    private List<DirectMessage> messageList = new ArrayList<>();

    private String recipientId;
    private String currentUserId;
    private String recipientFcmToken;
    private String currentUserFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_chat_activity);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();
        recipientId = getIntent().getStringExtra("senderId");

        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);

        setupRecyclerView();

        fetchRecipientToken();
        fetchCurrentUserInfo();

        loadMessages();

        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerViewMessages.setLayoutManager(linearLayoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void fetchRecipientToken() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(recipientId);
        userRef.child("fcmToken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipientFcmToken = snapshot.getValue(String.class);
                if (recipientFcmToken == null) {
                    Log.w(TAG, "El destinatario no tiene un token FCM registrado.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al obtener el token del destinatario: " + error.getMessage());
            }
        });
    }

    private void fetchCurrentUserInfo() {
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        currentUserRef.child("nombreCompleto").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserFullName = snapshot.getValue(String.class);
                if (currentUserFullName == null) {
                    Log.w(TAG, "No se pudo obtener el nombre del usuario actual. Se usará un nombre genérico.");
                    currentUserFullName = "Alguien";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al obtener el nombre del usuario actual: " + error.getMessage());
                currentUserFullName = "Alguien";
            }
        });
    }

    private void loadMessages() {
        if (currentUserId == null || recipientId == null) {
            Log.e(TAG, "Error: currentUserId o recipientId es nulo. No se pueden cargar mensajes.");
            Toast.makeText(this, "Error al cargar el chat.", Toast.LENGTH_SHORT).show();
            return;
        }
        String chatNode = getChatNode(currentUserId, recipientId);
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("direct_messages").child(chatNode);
        messagesRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    DirectMessage message = messageSnapshot.getValue(DirectMessage.class);
                    if (message != null) { // Comprobación de nulidad
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                recyclerViewMessages.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error al cargar los mensajes.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String content = editTextMessage.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        DatabaseManager dbManager = new DatabaseManager(this);
        // Pasamos el recipientId que ya tenemos en la actividad
        dbManager.sendDirectMessage(currentUserId, recipientId, content, new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                editTextMessage.setText("");
                sendPushNotification(content);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(ChatActivity.this, "Error al enviar el mensaje: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendPushNotification(String body) {
        if (recipientFcmToken == null || recipientFcmToken.isEmpty()) {
            Log.w(TAG, "No se envía notificación push porque el destinatario no tiene token.");
            return;
        }
        if (currentUserFullName == null) {
            Log.w(TAG, "No se envía notificación push porque el nombre del remitente aún no se ha cargado.");
            return;
        }

        String functionUrl = "https://capacitacion-mrodante.netlify.app/.netlify/functions/send-notification";
        JSONObject payload = new JSONObject();
        try {
            payload.put("isChatMessage", true);
            payload.put("senderId", currentUserId);
            payload.put("senderName", currentUserFullName);
            payload.put("recipientId", recipientId);
            payload.put("title", "Nuevo mensaje de " + currentUserFullName);
            payload.put("body", body);
            payload.put("token", recipientFcmToken);

        } catch (JSONException e) {
            Log.e(TAG, "Error creando JSON para notificación de chat", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, functionUrl, payload,
                response -> Log.d(TAG, "Petición de notificación enviada con éxito."),
                error -> Log.e(TAG, "Error al enviar la petición de notificación: " + error.toString()));
        Volley.newRequestQueue(this).add(request);
    }

    // *** INICIO DE LA CORRECCIÓN ***
    private String getChatNode(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) return "";
        // Se asegura que el ID del chat sea siempre el mismo, sin importar quién envía el mensaje.
        if (userId1.compareTo(userId2) > 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }
    // *** FIN DE LA CORRECCIÓN ***
}
