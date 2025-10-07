package metro.plascreem;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.UUID;

import metro.plascreem.databinding.FragmentCreateEventBinding;

public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;
    private DatabaseManager databaseManager;

    public CreateEventFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager();

        // Configurar el Spinner (desplegable) para el tipo de evento
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{ "Simple", "Documento", "Media" });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerEventType.setAdapter(adapter);

        // Configurar el listener del botón
        binding.btnSaveEvent.setOnClickListener(v -> saveEvent());

        return binding.getRoot();
    }

    private void saveEvent() {
        String title = binding.etEventTitle.getText().toString().trim();
        String description = binding.etEventDescription.getText().toString().trim();

        // Validar que el título no esté vacío
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, ingrese un título para el evento.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener la fecha del DatePicker
        int day = binding.dpEventDate.getDayOfMonth();
        int month = binding.dpEventDate.getMonth();
        int year = binding.dpEventDate.getYear();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        // Formatear la fecha a "YYYY-MM-DD"
        String date = String.format("%d-%02d-%02d", year, month + 1, day);

        // Obtener el tipo de acción del Spinner
        String eventType = (String) binding.spinnerEventType.getSelectedItem();
        String tipoAccion;
        switch (eventType) {
            case "Documento":
                tipoAccion = Evento.TIPO_DOCUMENTO;
                break;
            case "Media":
                tipoAccion = Evento.TIPO_MEDIA;
                break;
            default:
                tipoAccion = Evento.TIPO_SIMPLE;
                break;
        }

        // Crear el objeto Evento
        String eventId = UUID.randomUUID().toString();
        Evento newEvent = new Evento(eventId, title, description, date, tipoAccion);

        // Guardar el evento en Firebase
        databaseManager.saveEvent(newEvent, new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Evento guardado con éxito", Toast.LENGTH_SHORT).show();

                // ¡NUEVO! Enviar la notificación a través del servidor Netlify
                sendNotification(newEvent.getTitulo(), newEvent.getDescripcion());

                // Regresar al fragmento anterior (CalendarFragment)
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al guardar el evento: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendNotification(String title, String body) {
        // IMPORTANTE: Reemplaza esta URL con la URL de tu función de Netlify una vez desplegada.
        String functionUrl = "YOUR_NETLIFY_FUNCTION_URL_HERE";

        if (getContext() == null) {
            Log.e("Notification", "Contexto nulo, no se puede enviar la solicitud.");
            return;
        }

        com.android.volley.RequestQueue queue = Volley.newRequestQueue(getContext());

        JSONObject postData = new JSONObject();
        try {
            postData.put("title", "Nuevo Evento: " + title);
            postData.put("body", body.isEmpty() ? "Consulta los detalles en la app." : body);
        } catch (JSONException e) {
            Log.e("NotificationError", "Error al crear el JSON", e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, functionUrl, postData,
                response -> {
                    Log.d("Notification", "Solicitud de notificación enviada con éxito.");
                },
                error -> {
                    Log.e("Notification", "Error al enviar la solicitud de notificación: " + error.toString());
                }
        );

        queue.add(jsonObjectRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Limpiar la referencia al binding
    }
}