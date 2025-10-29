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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_chat_activity);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // CORRECCIÓN: Usar la clave estandarizada "senderId" que ahora viene del Login o del Servicio
        recipientId = getIntent().getStringExtra("senderId");

        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);

        setupRecyclerView();
        fetchRecipientToken();
        loadMessages();

        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerViewMessages.setLayoutManager(linearLayoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void fetchRecipientToken() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(recipientId);
        userRef.child("fcmToken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipientFcmToken = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al obtener el token del destinatario: " + error.getMessage());
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
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    DirectMessage message = messageSnapshot.getValue(DirectMessage.class);
                    messageList.add(message);
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

        String chatNode = getChatNode(currentUserId, recipientId);
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("direct_messages").child(chatNode);

        String messageId = messagesRef.push().getKey();
        DirectMessage message = new DirectMessage(messageId, currentUserId, recipientId, content, "message", System.currentTimeMillis());

        if (messageId != null) {
            messagesRef.child(messageId).setValue(message).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    editTextMessage.setText("");
                    sendPushNotification(content);
                }
            });
        }
    }

    private void sendPushNotification(String body) {
        if (recipientFcmToken == null || recipientFcmToken.isEmpty()) {
            return;
        }

        String functionUrl = "https://capacitacion-mrodante.netlify.app/.netlify/functions/send-notification";
        JSONObject payload = new JSONObject();
        try {
            payload.put("title", "Nuevo Mensaje");
            payload.put("body", body);
            payload.put("token", recipientFcmToken);
            // Esta es la clave que se envía en la notificación
            payload.put("senderId", currentUserId);
        } catch (JSONException e) {
            Log.e(TAG, "Error creando JSON para notificación", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, functionUrl, payload,
                response -> Log.d(TAG, "Notificación enviada con éxito."),
                error -> Log.e(TAG, "Error al enviar la notificación push: " + error.toString()));
        Volley.newRequestQueue(this).add(request);
    }

    private String getChatNode(String userId1, String userId2) {
        if (userId1.compareTo(userId2) > 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }
}

