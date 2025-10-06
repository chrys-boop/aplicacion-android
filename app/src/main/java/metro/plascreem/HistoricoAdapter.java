package metro.plascreem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder> {

    private final List<HistoricoArchivo> listaHistorico;

    public HistoricoAdapter(List<HistoricoArchivo> listaHistorico) {
        this.listaHistorico = listaHistorico;
    }

    @NonNull
    @Override
    public HistoricoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historico_archivo, parent, false);
        return new HistoricoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoricoViewHolder holder, int position) {
        HistoricoArchivo archivo = listaHistorico.get(position);

        holder.tvNombre.setText(archivo.getNombreArchivo() + "." + archivo.getExtension());
        holder.tvInfo.setText("Subido por: " + archivo.getUsuario() + " | Hora: " + archivo.getHoraSubida());

        // Simulación: Asignar icono basado en la extensión
        int iconRes = getFileIcon(archivo.getExtension());
        holder.ivIcon.setImageResource(iconRes);

        holder.btnDescargar.setOnClickListener(v -> {
            // TODO: Lógica de descarga simulada/real
            Toast.makeText(v.getContext(), "Iniciando descarga de " + archivo.getNombreArchivo(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return listaHistorico.size();
    }

    // Helper para simular el ícono del archivo
    private int getFileIcon(String ext) {
        String lowerExt = ext.toLowerCase();
        if (lowerExt.equals("pdf")) return android.R.drawable.ic_menu_revert;
        if (lowerExt.equals("jpg") || lowerExt.equals("png")) return android.R.drawable.ic_menu_gallery;
        if (lowerExt.equals("mp4") || lowerExt.equals("mov")) return android.R.drawable.ic_menu_slideshow;
        return android.R.drawable.ic_menu_view; // Icono genérico
    }

    public static class HistoricoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvInfo;
        ImageView ivIcon;
        ImageButton btnDescargar;

        public HistoricoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_historico_nombre);
            tvInfo = itemView.findViewById(R.id.tv_historico_info);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            btnDescargar = itemView.findViewById(R.id.btn_descargar);
        }
    }
}