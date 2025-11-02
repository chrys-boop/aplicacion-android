package metro.plascreem;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.RemoteInput;

public class NotificationReplyReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReply";
    private static final String KEY_TEXT_REPLY = "key_text_reply";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. Extraer el texto de la respuesta desde la notificación
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput == null) {
            Log.e(TAG, "No se encontró el input de la respuesta en el Intent.");
            return;
        }
        CharSequence replyText = remoteInput.getCharSequence(KEY_TEXT_REPLY);
        if (replyText == null || replyText.toString().trim().isEmpty()) {
            Log.w(TAG, "La respuesta está vacía, no se enviará nada.");
            return;
        }

        // 2. Obtener los IDs para saber quién envía y quién recibe
        String recipientId = intent.getStringExtra("senderId"); // El remitente original es ahora el destinatario
        String currentUserId = intent.getStringExtra("currentUserId");

        if (recipientId == null || currentUserId == null) {
            Log.e(TAG, "Faltan los IDs de usuario (recipientId o currentUserId).");
            return;
        }

        Log.d(TAG, "Respuesta rápida para: " + recipientId + ". Mensaje: '" + replyText + "'");

        // 3. Guardar el mensaje en la base de datos
        DatabaseManager dbManager = new DatabaseManager(context);
        dbManager.sendDirectMessage(currentUserId, recipientId, replyText.toString(), new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Respuesta rápida enviada a la base de datos correctamente.");

                // 4. (Opcional) Cancelar la notificación después de responder
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(recipientId, 1); // El ID debe coincidir con el usado en MyFirebaseMessagingService
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error al enviar la respuesta rápida a la base de datos: " + message);
                // Aquí podrías reintentar o notificar al usuario que la respuesta falló
            }
        });
    }
}

