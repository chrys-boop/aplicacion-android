package metro.plascreem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<FileMetadata> fileList;
    private final OnFileActionListener listener;
    private final boolean showDeleteButton;

    public interface OnFileActionListener {
        void onViewFile(FileMetadata file);
        void onDeleteFile(FileMetadata file);
    }

    public FileAdapter(List<FileMetadata> fileList, OnFileActionListener listener, boolean showDeleteButton) {
        this.fileList = fileList;
        this.listener = listener;
        this.showDeleteButton = showDeleteButton;
    }

    // Sobrecarga de constructor para mantener compatibilidad con AdminFilesFragment
    public FileAdapter(List<FileMetadata> fileList, OnFileActionListener listener) {
        this(fileList, listener, true); // Por defecto, mostrar el botón de eliminar
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
        holder.fileName.setText(file.getName());

        holder.viewButton.setOnClickListener(v -> listener.onViewFile(file));

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

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        ImageButton viewButton;
        ImageButton deleteButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.tv_file_name);
            viewButton = itemView.findViewById(R.id.btn_view_file);
            deleteButton = itemView.findViewById(R.id.btn_delete_file);
        }
    }
}