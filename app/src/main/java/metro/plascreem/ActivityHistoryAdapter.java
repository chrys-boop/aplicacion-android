package metro.plascreem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityHistoryAdapter extends RecyclerView.Adapter<ActivityHistoryAdapter.ViewHolder> {

    private final List<HistoricoArchivo> historyList;

    public ActivityHistoryAdapter(List<HistoricoArchivo> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoricoArchivo historyItem = historyList.get(position);
        Context context = holder.itemView.getContext();

        // Construir la descripción de la actividad
        String activityDescription = context.getString(R.string.activity_history_description_format,
                historyItem.getUserEmail(), historyItem.getFileName());
        holder.tvActivityDescription.setText(activityDescription);

        // Formatear y mostrar la fecha
        Long timestamp = historyItem.getTimestampLong();
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm:ss", Locale.getDefault());
            String formattedDate = sdf.format(new Date(timestamp));
            holder.tvActivityTimestamp.setText(formattedDate);
        } else {
            holder.tvActivityTimestamp.setText(R.string.data_not_available_short);
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvActivityDescription;
        final TextView tvActivityTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActivityDescription = itemView.findViewById(R.id.tv_activity_description);
            tvActivityTimestamp = itemView.findViewById(R.id.tv_activity_timestamp);
        }
    }
}
