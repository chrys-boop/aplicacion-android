package metro.plascreem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<FileMetadata> fileList;
    private final OnFileActionListener listener;
    private final boolean showDeleteButton;

    // --- Interfaz de acciones actualizada ---
    public interface OnFileActionListener {
        void onViewFile(FileMetadata file);
        void onDownloadFile(FileMetadata file);
        void onDeleteFile(FileMetadata file);
    }

    public FileAdapter(List<FileMetadata> fileList, OnFileActionListener listener, boolean showDeleteButton) {
        this.fileList = fileList;
        this.listener = listener;
        this.showDeleteButton = showDeleteButton;
    }

    public FileAdapter(List<FileMetadata> fileList, OnFileActionListener listener) {
        this(fileList, listener, true);
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileMetadata file = fileList.get(position);
        holder.fileName.setText(file.getFileName()); // Usar getFileName() para consistencia
        // Aquí se podrían popular otros campos como fecha y tamaño si los tuvieras en el holder

        // Asignar listeners a los nuevos botones
        holder.viewButton.setOnClickListener(v -> listener.onViewFile(file));
        holder.downloadButton.setOnClickListener(v -> listener.onDownloadFile(file));

        // Lógica para mostrar/ocultar el botón de eliminar
        if (showDeleteButton) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> listener.onDeleteFile(file));
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    // --- ViewHolder actualizado para usar MaterialButton ---
    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        // Otros TextViews como tv_upload_date, tv_file_size pueden ser añadidos aquí
        MaterialButton viewButton;
        MaterialButton downloadButton;
        MaterialButton deleteButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.tv_file_name);
            viewButton = itemView.findViewById(R.id.btn_view_file);
            downloadButton = itemView.findViewById(R.id.btn_download_file);
            deleteButton = itemView.findViewById(R.id.btn_delete_file);
        }
    }
}
