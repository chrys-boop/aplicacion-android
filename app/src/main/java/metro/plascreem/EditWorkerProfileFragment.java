package metro.plascreem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import metro.plascreem.databinding.FragmentEditWorkerProfileBinding;

public class EditWorkerProfileFragment extends Fragment {

    private FragmentEditWorkerProfileBinding binding;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    // Pattern for DD/MM/AAAA format
    private static final Pattern DATE_PATTERN = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditWorkerProfileBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager(getContext());
        mAuth = FirebaseAuth.getInstance();

        setupHourSpinners();
        loadProfileData();

        binding.btnGuardarPerfil.setOnClickListener(v -> saveProfile());

        return binding.getRoot();
    }

    private void setupHourSpinners() {
        List<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d:00", i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, hours);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spinnerHoraEntrada.setAdapter(adapter);
        binding.spinnerHoraSalida.setAdapter(adapter);
    }

    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (userData != null) {
                        binding.etNombre.setText(String.valueOf(userData.getOrDefault("nombre", "")));
                        binding.etApellidoPaterno.setText(String.valueOf(userData.getOrDefault("apellidoPaterno", "")));
                        binding.etApellidoMaterno.setText(String.valueOf(userData.getOrDefault("apellidoMaterno", "")));
                        binding.etExpediente.setText(String.valueOf(userData.getOrDefault("numeroExpediente", "")));
                        binding.etCategoria.setText(String.valueOf(userData.getOrDefault("categoria", "")));
                        binding.etFechaIngreso.setText(String.valueOf(userData.getOrDefault("fechaIngreso", "")));

                        String horarioEntrada = (String) userData.getOrDefault("horarioEntrada", "00:00");
                        String horarioSalida = (String) userData.getOrDefault("horarioSalida", "00:00");

                        setSpinnerSelection(binding.spinnerHoraEntrada, horarioEntrada);
                        setSpinnerSelection(binding.spinnerHoraSalida, horarioSalida);
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    Toast.makeText(getContext(), "Error al cargar los datos: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "No se ha iniciado sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String nombre = binding.etNombre.getText().toString().toUpperCase().trim();
        String apellidoPaterno = binding.etApellidoPaterno.getText().toString().toUpperCase().trim();
        String apellidoMaterno = binding.etApellidoMaterno.getText().toString().toUpperCase().trim();
        String nombreCompleto = nombre + " " + apellidoPaterno + " " + apellidoMaterno;
        String expediente = binding.etExpediente.getText().toString().toUpperCase().trim();
        String categoria = binding.etCategoria.getText().toString().toUpperCase().trim();
        String fechaIngreso = binding.etFechaIngreso.getText().toString().trim();

        if (!isValidDate(fechaIngreso)) {
            binding.etFechaIngreso.setError("Formato de fecha no válido. Use DD/MM/AAAA");
            return;
        }

        String horarioEntrada = (String) binding.spinnerHoraEntrada.getSelectedItem();
        String horarioSalida = (String) binding.spinnerHoraSalida.getSelectedItem();

        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || expediente.isEmpty() || categoria.isEmpty() || fechaIngreso.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("nombre", nombre);
        userProfile.put("apellidoPaterno", apellidoPaterno);
        userProfile.put("apellidoMaterno", apellidoMaterno);
        userProfile.put("nombreCompleto", nombreCompleto);
        userProfile.put("numeroExpediente", expediente);
        userProfile.put("categoria", categoria);
        userProfile.put("fechaIngreso", fechaIngreso);
        userProfile.put("horarioEntrada", horarioEntrada);
        userProfile.put("horarioSalida", horarioSalida);

        databaseManager.updateWorkerProfile(userId, userProfile, new DatabaseManager.DataSaveListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al actualizar el perfil: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidDate(String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
