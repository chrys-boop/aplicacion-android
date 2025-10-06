package metro.plascreem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ManualAdapter extends RecyclerView.Adapter<ManualAdapter.ManualViewHolder> {

    private List<Trabajadores.Manual> manualList;
    private OnManualClickListener listener;

    // La interfaz debe recibir dos parámetros
    public interface OnManualClickListener {
        void onManualClick(Trabajadores.Manual manual, String action);
    }

    public ManualAdapter(List<Trabajadores.Manual> manualList, OnManualClickListener listener) {
        this.manualList = manualList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ManualViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manual, parent, false);
        return new ManualViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManualViewHolder holder, int position) {
        Trabajadores.Manual manual = manualList.get(position);
        holder.tvTitle.setText(manual.getTitle());

        // Listener para visualizar el manual (al hacer clic en el título o en la fila)
        holder.itemView.setOnClickListener(v -> listener.onManualClick(manual, "visualizar"));

        // Listener para descargar el manual (al hacer clic en el ícono de descarga)
        holder.downloadIcon.setOnClickListener(v -> listener.onManualClick(manual, "descargar"));
    }

    @Override
    public int getItemCount() {
        return manualList.size();
    }

    static class ManualViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView downloadIcon;

        ManualViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_manual_title);
            downloadIcon = itemView.findViewById(R.id.iv_download_icon);
        }
    }
}