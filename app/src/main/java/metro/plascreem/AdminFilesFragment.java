package metro.plascreem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import metro.plascreem.databinding.FragmentAdminFilesBinding;

public class AdminFilesFragment extends Fragment {

    private FragmentAdminFilesBinding binding;
    private static final int PICK_FILE_REQUEST_CODE = 100;

    public AdminFilesFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminFilesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Lógica de Carga de Archivos ---
        binding.btnSeleccionarSubir.setOnClickListener(v -> {
            openGenericFileSelector();
        });

        // --- Lógica de Simulación de Visualización ---

        // 1. Simulación PDF/Documento (Solo muestra un mensaje)
        binding.btnFilePdf.setOnClickListener(v -> {
            simulateFileViewer("Manual de Seguridad.pdf", "application/pdf");
        });

        // 2. Simulación Video (Necesita un VideoView real, aquí solo simula)
        binding.btnFileVideo.setOnClickListener(v -> {
            simulateFileViewer("Video de Capacitación.mp4", "video/*");
        });

        // 3. Simulación Imagen (Solo muestra un mensaje)
        binding.btnFileImage.setOnClickListener(v -> {
            simulateFileViewer("Diagrama Eléctrico.jpg", "image/*");
        });

        // 4. Simulación Audio (Solo muestra un mensaje)
        binding.btnFileAudio.setOnClickListener(v -> {
            simulateFileViewer("Minuta de Reunión.mp3", "audio/*");
        });
    }

    // Método para abrir el selector de archivos genérico (para subida)
    private void openGenericFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Permitimos seleccionar cualquier tipo de archivo
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/pdf", "application/msword", "application/vnd.ms-excel",
                "video/*", "audio/*", "image/*"
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Selecciona Archivo para Subir"), PICK_FILE_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al abrir el selector de archivos.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            // Simulación de subida (mostrar el nombre del archivo seleccionado)
            Toast.makeText(getContext(), "Archivo seleccionado: " + fileUri.getLastPathSegment(), Toast.LENGTH_LONG).show();
            binding.tvSubidaStatus.setText("Listo para subir: " + fileUri.getLastPathSegment() + ".\n(Aquí iría la lógica de subida real)");

            // Aquí llamarías a una función para subir el archivo.
            // Por ahora, solo actualizamos el estado.
        }
    }

    // --- LÓGICA DE SIMULACIÓN DE VISUALIZACIÓN ---
    private void simulateFileViewer(String fileName, String mimeType) {
        binding.cardMediaPlayer.setVisibility(View.VISIBLE);

        // Limpiamos el contenedor anterior
        binding.mediaContainer.removeAllViews();

        // En una app real, aquí se descargaría el archivo de Supabase y se mostraría.

        if (mimeType.startsWith("image/")) {
            // Simulación: Muestra una ImageView (con un placeholder)
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setImageResource(android.R.drawable.ic_menu_gallery); // Placeholder
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            binding.mediaContainer.addView(imageView);
            Toast.makeText(getContext(), "Visualizando: " + fileName, Toast.LENGTH_SHORT).show();

        } else if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) {
            // Simulación: Muestra un VideoView o un mensaje para audio
            TextView textView = new TextView(getContext());
            textView.setText("Reproduciendo " + (mimeType.startsWith("video/") ? "VIDEO" : "AUDIO") + ": " + fileName + "\n(Aquí iría el VideoView/MediaPlayer)");
            textView.setGravity(android.view.Gravity.CENTER);
            textView.setTextSize(18);
            binding.mediaContainer.addView(textView);
            Toast.makeText(getContext(), "Reproduciendo: " + fileName, Toast.LENGTH_SHORT).show();

        } else if (mimeType.contains("pdf") || mimeType.contains("word") || mimeType.contains("excel")) {
            // Simulación: Muestra un mensaje para documentos
            TextView textView = new TextView(getContext());
            textView.setText("Documento " + fileName + " abierto.\n(En una app real, se usaría una librería de visor PDF/DOCX)");
            textView.setGravity(android.view.Gravity.CENTER);
            textView.setTextSize(18);
            binding.mediaContainer.addView(textView);
            Toast.makeText(getContext(), "Abriendo documento: " + fileName, Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}