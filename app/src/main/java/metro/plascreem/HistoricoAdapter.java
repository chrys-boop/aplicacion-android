package metro.plascreem;

import android.content.Context;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import metro.plascreem.databinding.ItemFileBinding;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.FileViewHolder> {

    private final List<FileMetadata> fileList;
    private final OnFileInteractionListener listener;

    // Interfaz para manejar los clics en los botones
    public interface OnFileInteractionListener {
        void onViewFile(FileMetadata file);
        void onDeleteFile(FileMetadata file);
    }

    public HistoricoAdapter(List<FileMetadata> fileList, OnFileInteractionListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usamos ViewBinding para inflar el layout correcto (item_file.xml)
        ItemFileBinding binding = ItemFileBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new FileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileMetadata file = fileList.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    // ViewHolder que usa ItemFileBinding
    static class FileViewHolder extends RecyclerView.ViewHolder {
        private final ItemFileBinding binding;

        public FileViewHolder(ItemFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final FileMetadata file, final OnFileInteractionListener listener) {
            Context context = binding.getRoot().getContext();

            // Poblar las vistas usando los IDs de item_file.xml
            binding.tvFileName.setText(file.getFileName());

            // Formatear y mostrar fecha y tamaño en sus TextViews separados
            String fileSize = Formatter.formatShortFileSize(context, file.getSize());
            binding.tvFileSize.setText(String.format("(%s)", fileSize));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(file.getTimestamp()));
            binding.tvUploadDate.setText(formattedDate);

            // Asignar el icono correcto
            binding.ivFileIcon.setImageResource(getFileIcon(file.getFileName()));

            // Asignar los listeners de los botones a la interfaz
            binding.btnViewFile.setOnClickListener(v -> listener.onViewFile(file));
            binding.btnDeleteFile.setOnClickListener(v -> listener.onDeleteFile(file));
        }

        private int getFileIcon(String fileName) {
            if (fileName == null) return R.drawable.ic_file_generic;
            String lowerName = fileName.toLowerCase();

            if (lowerName.endsWith(".pdf")) return R.drawable.ic_file_pdf;
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) return R.drawable.ic_file_image;
            if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) return R.drawable.ic_file_excel;
            if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) return R.drawable.ic_file_word;
            if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov")) return R.drawable.ic_file_video;

            return R.drawable.ic_file_generic;
        }
    }
}