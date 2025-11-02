package metro.plascreem;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "fcm_default_channel";
    private static final String KEY_TEXT_REPLY = "key_text_reply";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message Data payload: " + data);

        String title = data.getOrDefault("title", "Nueva notificación");
        String body = data.getOrDefault("body", "");

        boolean isChatMessage = "true".equals(data.get("isChatMessage"));

        if (isChatMessage) {
            String senderId = data.get("senderId");
            String senderName = data.get("senderName");
            String recipientId = data.get("recipientId"); // ID del usuario de este dispositivo

            if (senderId != null && recipientId != null) {
                sendChatNotification(title, body, senderId, senderName, recipientId);
            }
        } else {
            String senderIdForIntent = data.get("senderId");
            sendGeneralNotification(title, body, senderIdForIntent);
        }
    }

    // *** MÉTODO CORREGIDO ***
    private void sendChatNotification(String title, String messageBody, String senderId, String senderName, String recipientId) {
        // --- 1. Crear el RemoteInput para la respuesta rápida ---
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Escribe una respuesta...")
                .build();

        // --- 2. Crear el PendingIntent para el BroadcastReceiver que procesará la respuesta ---
        Intent replyIntent = new Intent(this, NotificationReplyReceiver.class);
        // Cuando respondamos, el que envía la respuesta es el usuario de este dispositivo (el recipientId original)
        replyIntent.putExtra("senderId", recipientId);
        // El que recibe la respuesta es el que envió el mensaje original (el senderId original)
        replyIntent.putExtra("recipientId", senderId);

        int requestCode = new Random().nextInt();
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // --- 3. Crear la acción de "Responder" ---
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_send,
                "Responder",
                replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        // --- 4. Crear el PendingIntent para cuando se toca la notificación (abrir el chat) ---
        Intent openChatIntent = new Intent(this, ChatActivity.class);
        openChatIntent.putExtra("senderId", senderId); // Al abrir el chat, el "otro" es el que nos envió el mensaje
        openChatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent openChatPendingIntent = PendingIntent.getActivity(this, requestCode + 1, openChatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // --- 5. Construir la notificación con estilo de mensajería ---
        Person user = new Person.Builder().setName(senderName).build();
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(user)
                .addMessage(messageBody, System.currentTimeMillis(), user);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_message)
                .setStyle(style)
                .setContentTitle(title)
                .setContentText(messageBody)
                .addAction(replyAction)
                .setContentIntent(openChatPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // --- 6. Enviar la notificación ---
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(notificationManager);
        notificationManager.notify(senderId, 1, notificationBuilder.build()); // ID de notificación por conversación
    }

    private void sendGeneralNotification(String messageTitle, String messageBody, String senderId) {
        Intent intent = new Intent(this, LoginActivity.class);
        if (senderId != null && !senderId.isEmpty()) {
            intent.putExtra("senderId", senderId);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int requestCode = new Random().nextInt();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(notificationManager);
        notificationManager.notify(requestCode, notificationBuilder.build());
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Notificaciones",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Canal para todas las notificaciones de la app");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseManager dbManager = new DatabaseManager(this);
            dbManager.updateUserFcmToken(currentUser.getUid(), token, new DatabaseManager.DataSaveListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "FCM token updated successfully on server.");
                }

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, "Failed to update FCM token on server: " + message);
                }
            });
        }
    }
}




