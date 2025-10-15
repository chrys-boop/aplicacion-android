package metro.plascreem; // Tu paquete principal

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class EventosAdapter extends RecyclerView.Adapter<EventosAdapter.EventoViewHolder> {

    private final List<Evento> eventosList;
    private final EventoActionListener listener; // Interfaz para notificar el click

    // --- MODIFICACIÓN --- Interfaz de comunicación actualizada
    public interface EventoActionListener {
        void onActionClick(Evento evento);
        void onEventoClicked(Evento evento); // <-- NUEVO MÉTODO
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
        MaterialButton btnAccion;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_item_titulo);
            tvDescripcion = itemView.findViewById(R.id.tv_item_descripcion);
            btnAccion = itemView.findViewById(R.id.btn_accion_evento);
        }

        public void bind(final Evento evento, final EventoActionListener listener) {
            tvTitulo.setText(evento.getTitulo());
            tvDescripcion.setText(evento.getDescripcion());

            // --- MODIFICACIÓN --- Listener para el clic en todo el elemento
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventoClicked(evento);
                }
            });

            String tipo = evento.getTipoAccion();

            // Lógica del botón de acción
            if (tipo.equals(Evento.TIPO_DOCUMENTO) || tipo.equals(Evento.TIPO_MEDIA)) {
                btnAccion.setVisibility(View.VISIBLE);
                String textoBoton = tipo.equals(Evento.TIPO_DOCUMENTO) ? "Subir Documento" : "Subir Media";
                btnAccion.setText(textoBoton);
                btnAccion.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onActionClick(evento);
                    }
                });
            } else {
                btnAccion.setVisibility(View.GONE);
                btnAccion.setOnClickListener(null);
            }
        }
    }
}