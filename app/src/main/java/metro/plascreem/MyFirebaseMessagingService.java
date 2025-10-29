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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = "";
        String body = "";
        String senderId = null;

        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            Log.d(TAG, "Message Data payload: " + data);
            title = data.get("title");
            body = data.get("body");
            senderId = data.get("senderId");
        }

        sendNotification(title, body, senderId);
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
            dbManager.updateUserFcmToken(currentUser.getUid(), token, null);
        }
    }

    private void sendNotification(String messageTitle, String messageBody, String senderId) {
        // *** INICIO DE LA SOLUCIÓN DIRECTA Y DEFINITIVA ***
        // El Intent ahora apunta DIRECTAMENTE a LoginActivity, eliminando intermediarios.
        Intent intent = new Intent(this, LoginActivity.class);

        // Si es una notificación de chat, añadimos el senderId. LoginActivity ya sabe qué hacer con él.
        if (senderId != null && !senderId.isEmpty()) {
            intent.putExtra("senderId", senderId);
        }

        // Estas flags aseguran que la app se comporte correctamente.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // *** FIN DE LA SOLUCIÓN DIRECTA Y DEFINITIVA ***

        // Usamos un requestCode aleatorio para asegurar que cada PendingIntent es único.
        int requestCode = new Random().nextInt();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este ícono existe
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Notificaciones Generales",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(requestCode, notificationBuilder.build());
    }
}



