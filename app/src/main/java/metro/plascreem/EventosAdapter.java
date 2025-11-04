package metro.plascreem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EventosAdapter extends RecyclerView.Adapter<EventosAdapter.EventoViewHolder> {

    private final List<Evento> eventosList;
    private final EventoActionListener listener;

    public interface EventoActionListener {
        void onActionClick(Evento evento);
        void onEventoClicked(Evento evento);
    }

    public EventosAdapter(List<Evento> eventosList, EventoActionListener listener) {
        this.eventosList = eventosList;
        this.listener = listener;
    }

    public void setEventos(List<Evento> newEvents) {
        eventosList.clear();
        eventosList.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evento, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Evento evento = eventosList.get(position);
        holder.bind(evento, listener);
    }

    @Override
    public int getItemCount() {
        return eventosList.size();
    }

    public static class EventoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo;
        TextView tvDescripcion;
        TextView tvEventDate; // Nuevo
        TextView tvCreationDate; // Nuevo
        MaterialButton btnAccion;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_item_titulo);
            tvDescripcion = itemView.findViewById(R.id.tv_item_descripcion);
            btnAccion = itemView.findViewById(R.id.btn_accion_evento);
            tvEventDate = itemView.findViewById(R.id.tv_event_date); // Nuevo
            tvCreationDate = itemView.findViewById(R.id.tv_creation_date); // Nuevo
        }

        public void bind(final Evento evento, final EventoActionListener listener) {
            tvTitulo.setText(evento.getTitulo());
            tvDescripcion.setText(evento.getDescripcion());

            // --- Lógica de fechas ---
            tvEventDate.setText(evento.getFecha());

            if (evento.getCreationTimestamp() > 0) {
                tvCreationDate.setText(formatTimestamp(evento.getCreationTimestamp()));
                // Hacemos visible el contenedor de la fecha de creación
                itemView.findViewById(R.id.tv_creation_date).setVisibility(View.VISIBLE);
                itemView.findViewById(R.id.tv_creation_date).getRootView().findViewById(R.id.tv_creation_date).setVisibility(View.VISIBLE);

            } else {
                // Ocultamos si no hay timestamp
                itemView.findViewById(R.id.tv_creation_date).setVisibility(View.GONE);
                itemView.findViewById(R.id.tv_creation_date).getRootView().findViewById(R.id.tv_creation_date).setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventoClicked(evento);
                }
            });

            String tipo = evento.getTipoAccion();

            if (tipo != null && (tipo.equals(Evento.TIPO_DOCUMENTO) || tipo.equals(Evento.TIPO_MEDIA))) {
                btnAccion.setVisibility(View.VISIBLE);
                String textoBoton = tipo.equals(Evento.TIPO_DOCUMENTO) ? "Documento" : "Media";
                btnAccion.setText(textoBoton);

                btnAccion.setOnClickListener(v -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    try {
                        Date eventDate = sdf.parse(evento.getFecha());
                        Calendar deadline = Calendar.getInstance();
                        if (eventDate != null) {
                            deadline.setTime(eventDate);
                        }

                        deadline.set(Calendar.HOUR_OF_DAY, 23);
                        deadline.set(Calendar.MINUTE, 59);
                        deadline.set(Calendar.SECOND, 59);

                        if (Calendar.getInstance().after(deadline)) {
                            Toast.makeText(itemView.getContext(), "La fecha límite para subir el archivo ha expirado.", Toast.LENGTH_LONG).show();
                            return;
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                        Toast.makeText(itemView.getContext(), "Error en el formato de fecha del evento.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (listener != null) {
                        listener.onActionClick(evento);
                    }
                });
            } else {
                btnAccion.setVisibility(View.GONE);
                btnAccion.setOnClickListener(null);
            }
        }

        private String formatTimestamp(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            // El timestamp de Firebase es UTC. Lo convertimos a la zona horaria local del dispositivo.
            Date date = new Date(timestamp);
            return sdf.format(date);
        }
    }
}
