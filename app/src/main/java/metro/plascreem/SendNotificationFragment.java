package metro.plascreem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SendNotificationFragment extends Fragment {

    private EditText etNotificationTitle, etNotificationMessage;
    private Button btnSendNotification;
    private RequestQueue requestQueue;

    // --- ¡CLAVE YA INSERTADA! ---
    private final String FCM_SERVER_KEY = "BCIGKBSueN26-106y122fTCtA85RQQ7_-Jmy1LsLhXiBPeAtS-tpu4gMq-tkAv67594iUeQN0rNEhpxtDR6mRUE";
    private final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_notification, container, false);

        etNotificationTitle = view.findViewById(R.id.et_notification_title);
        etNotificationMessage = view.findViewById(R.id.et_notification_message);
        btnSendNotification = view.findViewById(R.id.btn_send_notification);

        requestQueue = Volley.newRequestQueue(requireContext());

        btnSendNotification.setOnClickListener(v -> sendNotification());

        return view;
    }

    private void sendNotification() {
        String title = etNotificationTitle.getText().toString().trim();
        String message = etNotificationMessage.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("title", title);
            notificationBody.put("body", message);
        } catch (JSONException e) {
            Toast.makeText(getContext(), "Error al crear la notificación", Toast.LENGTH_SHORT).show();
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("to", "/topics/all");
            requestBody.put("notification", notificationBody);
        } catch (JSONException e) {
            Toast.makeText(getContext(), "Error al crear la petición", Toast.LENGTH_SHORT).show();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, FCM_API_URL, requestBody,
                response -> {
                    Toast.makeText(getContext(), "Notificación enviada correctamente", Toast.LENGTH_SHORT).show();
                    etNotificationTitle.setText("");
                    etNotificationMessage.setText("");
                },
                error -> {
                    Toast.makeText(getContext(), "Error al enviar la notificación: " + error.toString(), Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "key=" + FCM_SERVER_KEY);
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }
}
