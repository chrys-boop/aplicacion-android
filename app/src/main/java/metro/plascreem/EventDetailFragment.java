package metro.plascreem;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class EventDetailFragment extends Fragment {

    private static final String ARG_EVENTO = "evento";
    private Evento evento;

    // Vistas
    private TextView tvTitle, tvDate, tvDescription, tvType;

    public static EventDetailFragment newInstance(Evento evento) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_EVENTO, evento);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            evento = getArguments().getParcelable(ARG_EVENTO);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tv_event_detail_title);
        tvDate = view.findViewById(R.id.tv_event_detail_date);
        tvDescription = view.findViewById(R.id.tv_event_detail_description);
        tvType = view.findViewById(R.id.tv_event_detail_type);

        if (evento != null) {
            tvTitle.setText(evento.getTitulo());
            tvDate.setText("Fecha: " + evento.getFecha());
            tvDescription.setText(evento.getDescripcion());

            // --- CORRECCIÓN Y MEJORA DE VISIBILIDAD ---
            String tipo = evento.getTipoAccion();
            tvType.setText(tipo);

            // Crear un fondo redondeado para la etiqueta
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(16); // Bordes redondeados

            // Asignar color según el tipo de evento
            int backgroundColor = Color.LTGRAY; // Color por defecto
            if (tipo != null) {
                switch (tipo) {
                    case Evento.TIPO_DOCUMENTO:
                        backgroundColor = ContextCompat.getColor(getContext(), R.color.colorDocument);
                        break;
                    case Evento.TIPO_MEDIA:
                        backgroundColor = ContextCompat.getColor(getContext(), R.color.colorMedia);
                        break;
                    case Evento.TIPO_SIMPLE:
                        backgroundColor = ContextCompat.getColor(getContext(), R.color.colorSimple);
                        break;
                }
            }
            shape.setColor(backgroundColor);
            tvType.setBackground(shape);
        }
    }
}