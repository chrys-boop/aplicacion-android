package metro.plascreem;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Clase de utilidad para mostrar Toasts personalizados en toda la aplicación.
 */
public class ToastUtils {

    /**
     * Muestra un Toast personalizado con una duración corta.
     *
     * @param context El contexto, idealmente el de la Actividad.
     * @param message El mensaje que se mostrará en el Toast.
     */
    public static void showShortToast(Activity context, String message) {
        if (context == null || message == null || message.isEmpty()) {
            return; // No hacer nada si el contexto o el mensaje son nulos/vacíos.
        }

        // Inflar el layout personalizado.
        LayoutInflater inflater = context.getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, context.findViewById(R.id.custom_toast_container));

        // Establecer el texto del mensaje.
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        // Crear y mostrar el Toast.
        Toast toast = new Toast(context.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100); // Posición en la pantalla.
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    /**
     * Muestra un Toast personalizado con una duración larga.
     *
     * @param context El contexto, idealmente el de la Actividad.
     * @param message El mensaje que se mostrará en el Toast.
     */
    public static void showLongToast(Activity context, String message) {
        if (context == null || message == null || message.isEmpty()) {
            return; // No hacer nada si el contexto o el mensaje son nulos/vacíos.
        }

        // Inflar el layout personalizado.
        LayoutInflater inflater = context.getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, context.findViewById(R.id.custom_toast_container));

        // Establecer el texto del mensaje.
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        // Crear y mostrar el Toast.
        Toast toast = new Toast(context.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100); // Posición en la pantalla.
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }
}
