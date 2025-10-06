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

    // Interfaz de comunicación: EL FRAGMENTO DEBE IMPLEMENTAR ESTO
    public interface EventoActionListener {
        void onActionClick(Evento evento);
    }

    public EventosAdapter(List<Evento> eventosList, EventoActionListener listener) {
        this.eventosList = eventosList;
        this.listener = listener;
    }

    public void setEventos(List<Evento> newEvents) {
        eventosList.clear();
        eventosList.addAll(newEvents);
        notifyDataSetChanged(); // Notificar a la lista que hay datos nuevos
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

            String tipo = evento.getTipoAccion();

            // Lógica clave: Mostrar el botón solo si requiere subir algo
            if (tipo.equals(Evento.TIPO_DOCUMENTO) || tipo.equals(Evento.TIPO_MEDIA)) {
                btnAccion.setVisibility(View.VISIBLE);

                String textoBoton = tipo.equals(Evento.TIPO_DOCUMENTO) ? "Subir Documento" : "Subir Media";
                btnAccion.setText(textoBoton);

                // Al hacer click, llama al Fragmento usando el listener
                btnAccion.setOnClickListener(v -> {
                    listener.onActionClick(evento);
                });
            } else {
                btnAccion.setVisibility(View.GONE);
                btnAccion.setOnClickListener(null);
            }
        }
    }
}