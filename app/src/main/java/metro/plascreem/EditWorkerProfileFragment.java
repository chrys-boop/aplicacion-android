package metro.plascreem;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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

public class EditWorkerProfileFragment extends Fragment {

    // --- CAMPOS DE VISTA ALINEADOS CON EditProfileFragment ---
    private EditText etNombreCompleto, etExpediente, etArea, etCargo, etFechaIngreso;
    private Spinner spinnerTitularType, spinnerHoraEntrada, spinnerHoraSalida;
    private AutoCompleteTextView spinnerCategoria;
    private Button btnGuardar;
    private DatabaseManager databaseManager;
    private ExcelManager excelManager;
    private FirebaseAuth mAuth;

    private static final Pattern DATE_PATTERN = Pattern.compile("^([0-9]{2})/([0-9]{2})/([0-9]{4})$");

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseManager = new DatabaseManager(requireContext());
        excelManager = new ExcelManager(requireContext());
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_worker_profile, container, false);

        // --- INICIALIZACIÓN DE VISTAS CON findViewById ---
        etNombreCompleto = view.findViewById(R.id.et_nombre_completo);
        etExpediente = view.findViewById(R.id.et_expediente);
        etArea = view.findViewById(R.id.et_area);
        etCargo = view.findViewById(R.id.et_cargo);
        etFechaIngreso = view.findViewById(R.id.et_fecha_ingreso);
        spinnerTitularType = view.findViewById(R.id.spinner_titular_type);
        spinnerHoraEntrada = view.findViewById(R.id.spinner_hora_entrada);
        spinnerHoraSalida = view.findViewById(R.id.spinner_hora_salida);
        spinnerCategoria = view.findViewById(R.id.spinner_categoria);
        btnGuardar = view.findViewById(R.id.btn_guardar_perfil);

        setupSpinners();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserData();
        btnGuardar.setOnClickListener(v -> showConfirmationDialog());
    }

    private void setupSpinners() {
        if (getContext() == null) return;
        ArrayAdapter<CharSequence> titularAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.titular_options, android.R.layout.simple_spinner_item);
        titularAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTitularType.setAdapter(titularAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.categorias, android.R.layout.simple_spinner_item);
        spinnerCategoria.setAdapter(categoryAdapter);

        List<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d:00", i));
        }
        ArrayAdapter<String> hoursAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, hours);
        hoursAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHoraEntrada.setAdapter(hoursAdapter);
        spinnerHoraSalida.setAdapter(hoursAdapter);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> firebaseData) {
                    if (firebaseData != null && isAdded()) {
                        String expediente = Objects.toString(firebaseData.get("numeroExpediente"), "");
                        etExpediente.setText(expediente);

                        excelManager.findUserByExpediente(expediente, new ExcelManager.ExcelDataListener() {
                            @Override
                            public void onDataFound(Map<String, Object> excelData) {
                                if (isAdded()) { populateFields(excelData); }
                            }

                            @Override
                            public void onDataNotFound() {
                                if (isAdded()) { populateFields(firebaseData); }
                            }

                            @Override
                            public void onError(String message) {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Error al cargar datos de Excel: " + message, Toast.LENGTH_SHORT).show();
                                    populateFields(firebaseData);
                                }
                            }
                        });
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
        etNombreCompleto.setText(Objects.toString(data.get("nombreCompleto"), "").toUpperCase());
        etArea.setText(Objects.toString(data.get("area"), "").toUpperCase());
        etCargo.setText(Objects.toString(data.get("cargo"), "").toUpperCase());
        spinnerCategoria.setText(Objects.toString(data.get("categoria"), ""), false);
        etFechaIngreso.setText(Objects.toString(data.get("fechaIngreso"), ""));

        setSpinnerSelection(spinnerTitularType, Objects.toString(data.get("titular"), ""));
        setSpinnerSelection(spinnerHoraEntrada, Objects.toString(data.get("horarioEntrada"), ""));
        setSpinnerSelection(spinnerHoraSalida, Objects.toString(data.get("horarioSalida"), ""));
    }

    private void showConfirmationDialog() {
        String nombreCompleto = etNombreCompleto.getText().toString().toUpperCase().trim();
        String expediente = etExpediente.getText().toString().toUpperCase().trim();
        String categoria = spinnerCategoria.getText().toString().trim();
        String fechaIngreso = etFechaIngreso.getText().toString().trim();

        if (nombreCompleto.isEmpty() || expediente.isEmpty() || fechaIngreso.isEmpty() || categoria.isEmpty() || categoria.equalsIgnoreCase("Seleccione una categoria")) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidDate(fechaIngreso)) {
            etFechaIngreso.setError("Formato no válido. Use DD/MM/AAAA");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Cambios")
                .setMessage("¿Estás seguro de que deseas guardar la información? Por favor, verifica que todos los datos sean correctos.")
                .setPositiveButton("Guardar", (dialog, which) -> saveProfileData())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void saveProfileData() {
        String fullName = etNombreCompleto.getText().toString().toUpperCase().trim();
        String expediente = etExpediente.getText().toString().toUpperCase().trim();
        String area = etArea.getText().toString().toUpperCase().trim();
        String cargo = etCargo.getText().toString().toUpperCase().trim();
        String categoria = spinnerCategoria.getText().toString().toUpperCase().trim();
        String fechaIngreso = etFechaIngreso.getText().toString().trim();
        String titularType = spinnerTitularType.getSelectedItem().toString();
        String horaEntrada = spinnerHoraEntrada.getSelectedItem().toString();
        String horaSalida = spinnerHoraSalida.getSelectedItem().toString();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Map<String, Object> userData = new HashMap<>();
            userData.put("nombreCompleto", fullName);
            userData.put("numeroExpediente", expediente);
            userData.put("area", area);
            userData.put("cargo", cargo);
            userData.put("categoria", categoria);
            userData.put("titular", titularType);
            userData.put("fechaIngreso", fechaIngreso);
            userData.put("horarioEntrada", horaEntrada);
            userData.put("horarioSalida", horaSalida);

            excelManager.saveUserData(userData, new DatabaseManager.DataSaveListener() {
                @Override
                public void onSuccess() {
                    databaseManager.updateWorkerProfile(userId, userData, new DatabaseManager.DataSaveListener() {
                        @Override
                        public void onSuccess() {
                            if (isAdded() && getActivity() != null) {
                                Toast.makeText(getContext(), "Perfil de trabajador actualizado.", Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().popBackStack();
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error al actualizar en Firebase: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al guardar en Excel: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private boolean isValidDate(String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) return false;
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
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (spinner.getAdapter().getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}

