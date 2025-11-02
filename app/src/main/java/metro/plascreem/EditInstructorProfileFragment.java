package metro.plascreem;

import android.app.AlertDialog;
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
import java.util.Objects;
import java.util.regex.Pattern;

import metro.plascreem.databinding.FragmentEditInstructorProfileBinding;

public class EditInstructorProfileFragment extends Fragment {

    private FragmentEditInstructorProfileBinding binding;
    private DatabaseManager databaseManager;
    private ExcelManager excelManager;
    private FirebaseAuth mAuth;

    private static final Pattern DATE_PATTERN = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditInstructorProfileBinding.inflate(inflater, container, false);
        databaseManager = new DatabaseManager(getContext());
        excelManager = new ExcelManager(getContext());
        mAuth = FirebaseAuth.getInstance();

        setupHourSpinners();
        setupCategorySpinner();
        loadProfileData();

        binding.btnGuardarPerfil.setOnClickListener(v -> showConfirmationDialog());

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

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.categorias, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategoria.setAdapter(adapter);
    }

    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> firebaseData) {
                    if (firebaseData != null && isAdded()) {
                        String expediente = Objects.toString(firebaseData.get("numeroExpediente"), "");
                        binding.etExpediente.setText(expediente);
                        Map<String, Object> excelData = excelManager.findUserByExpediente(expediente);
                        Map<String, Object> dataToUse = (excelData != null) ? excelData : firebaseData;
                        populateFields(dataToUse);
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al cargar datos: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void populateFields(Map<String, Object> data) {
        String nombreCompleto = Objects.toString(data.get("nombreCompleto"), "");
        String[] nameParts = nombreCompleto.split(" ", 3);
        binding.etNombre.setText(nameParts.length > 0 ? nameParts[0].toUpperCase() : "");
        binding.etApellidoPaterno.setText(nameParts.length > 1 ? nameParts[1].toUpperCase() : "");
        binding.etApellidoMaterno.setText(nameParts.length > 2 ? nameParts[2].toUpperCase() : "");

        String categoria = Objects.toString(data.get("categoria"), "");
        binding.spinnerCategoria.setText(categoria.toUpperCase(), false);

        binding.etFechaIngreso.setText(Objects.toString(data.get("fechaIngreso"), ""));

        setSpinnerSelection(binding.spinnerHoraEntrada, Objects.toString(data.get("horarioEntrada"), ""));
        setSpinnerSelection(binding.spinnerHoraSalida, Objects.toString(data.get("horarioSalida"), ""));
    }

    private void showConfirmationDialog() {
        String nombre = binding.etNombre.getText().toString().toUpperCase().trim();
        String apellidoPaterno = binding.etApellidoPaterno.getText().toString().toUpperCase().trim();
        String expediente = binding.etExpediente.getText().toString().toUpperCase().trim();
        String categoria = binding.spinnerCategoria.getText().toString().trim();
        String fechaIngreso = binding.etFechaIngreso.getText().toString().trim();

        if (nombre.isEmpty() || apellidoPaterno.isEmpty() || expediente.isEmpty() || fechaIngreso.isEmpty() || categoria.isEmpty() || categoria.equalsIgnoreCase("Seleccione una categoria")) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            if (categoria.isEmpty() || categoria.equalsIgnoreCase("Seleccione una categoria")) {
                binding.spinnerCategoria.setError("Debe seleccionar una categoría");
            }
            return;
        }

        if (!isValidDate(fechaIngreso)) {
            binding.etFechaIngreso.setError("Formato de fecha no válido. Use DD/MM/AAAA");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Cambios")
                .setMessage("¿Estás seguro de que deseas guardar la información? Por favor, verifica que todos los datos sean correctos.")
                .setPositiveButton("Guardar", (dialog, which) -> saveProfile())
                .setNegativeButton("Cancelar", null)
                .show();
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
        String nombreCompleto = (nombre + " " + apellidoPaterno + " " + apellidoMaterno).trim();
        String expediente = binding.etExpediente.getText().toString().toUpperCase().trim();
        String categoria = binding.spinnerCategoria.getText().toString().toUpperCase().trim();
        String fechaIngreso = binding.etFechaIngreso.getText().toString().trim();
        String horarioEntrada = (String) binding.spinnerHoraEntrada.getSelectedItem();
        String horarioSalida = (String) binding.spinnerHoraSalida.getSelectedItem();

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

        excelManager.saveUserData(userProfile);

        // <<< INICIO: CORRECCIÓN A MÉTODO EXISTENTE >>>
        databaseManager.updateWorkerProfile(userId, userProfile, new DatabaseManager.DataSaveListener() {
            // <<< FIN: CORRECCIÓN >>>
            @Override
            public void onSuccess() {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getContext(), "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(String message) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error al actualizar en Firebase: " + message, Toast.LENGTH_LONG).show();
                }
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
        if (value == null || value.isEmpty() || spinner.getAdapter() == null) return;
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equalsIgnoreCase(value)) {
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
