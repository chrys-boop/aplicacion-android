package metro.plascreem;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExcelManager {

    private static final String TAG = "ExcelManager";
    // CORRECCIÓN: Usar el nombre de archivo que hemos estado referenciando
    private static final String FILE_NAME = "datos_excel.xlsx";
    private final File localFile;
    private final StorageReference storageReference;

    // Column mapping (Asegúrate de que coincida con tu estructura)
    private static final int COL_EXPEDIENTE = 0;
    private static final int COL_NOMBRE_COMPLETO = 1;
    private static final int COL_NOMBRE = 2;
    private static final int COL_APELLIDO_PATERNO = 3;
    private static final int COL_APELLIDO_MATERNO = 4;
    private static final int COL_AREA = 5;
    private static final int COL_CATEGORIA = 6;
    private static final int COL_CARGO = 7;
    private static final int COL_FECHA_INGRESO = 8;
    private static final int COL_HORARIO_ENTRADA = 9;
    private static final int COL_HORARIO_SALIDA = 10;
    private static final int COL_TITULAR_TYPE = 11;
    private static final int COL_ENLACE_ORIGEN = 12; // Añadido


    public ExcelManager(Context context) {
        localFile = new File(context.getCacheDir(), FILE_NAME);
        // CORRECCIÓN: La ruta en Storage debe ser consistente
        storageReference = FirebaseStorage.getInstance().getReference().child("documentos/" + FILE_NAME);
    }

    public interface ExcelDataListener {
        void onDataFound(Map<String, Object> data);
        void onDataNotFound();
        void onError(String message);
    }

    public void findUserByExpediente(String expediente, ExcelDataListener listener) {
        storageReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            try (FileInputStream fis = new FileInputStream(localFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    Cell expedienteCell = row.getCell(COL_EXPEDIENTE);
                    if (expedienteCell != null && getCellStringValue(expedienteCell).equalsIgnoreCase(expediente)) {
                        listener.onDataFound(extractUserDataFromRow(row));
                        return;
                    }
                }
                listener.onDataNotFound();
            } catch (IOException e) {
                Log.e(TAG, "Error reading Excel file.", e);
                listener.onError(e.getMessage());
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error downloading Excel file for findUserByExpediente.", e);
            listener.onError(e.getMessage());
        });
    }

    private Map<String, Object> extractUserDataFromRow(Row row) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("numeroExpediente", getCellStringValue(row.getCell(COL_EXPEDIENTE)));
        userData.put("nombreCompleto", getCellStringValue(row.getCell(COL_NOMBRE_COMPLETO)));
        userData.put("nombre", getCellStringValue(row.getCell(COL_NOMBRE)));
        userData.put("apellidoPaterno", getCellStringValue(row.getCell(COL_APELLIDO_PATERNO)));
        userData.put("apellidoMaterno", getCellStringValue(row.getCell(COL_APELLIDO_MATERNO)));
        userData.put("area", getCellStringValue(row.getCell(COL_AREA)));
        userData.put("categoria", getCellStringValue(row.getCell(COL_CATEGORIA)));
        userData.put("cargo", getCellStringValue(row.getCell(COL_CARGO)));
        userData.put("fechaIngreso", getCellStringValue(row.getCell(COL_FECHA_INGRESO)));
        userData.put("horarioEntrada", getCellStringValue(row.getCell(COL_HORARIO_ENTRADA)));
        userData.put("horarioSalida", getCellStringValue(row.getCell(COL_HORARIO_SALIDA)));
        userData.put("titular", getCellStringValue(row.getCell(COL_TITULAR_TYPE))); // Clave 'titular' que usa la app
        userData.put("enlaceOrigen", getCellStringValue(row.getCell(COL_ENLACE_ORIGEN)));
        return userData;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }

    // --- MÉTODO saveUserData CORREGIDO Y ROBUSTO ---
    public void saveUserData(Map<String, Object> userData, DatabaseManager.DataSaveListener listener) {
        // Paso 1: Intentar descargar el archivo existente
        storageReference.getFile(localFile).addOnSuccessListener(downloadTask -> {
            // Éxito: El archivo existe y se descargó. Procedemos a modificarlo.
            Log.d(TAG, "Excel file downloaded successfully. Proceeding to modify.");
            modifyAndUploadWorkbook(userData, listener);

        }).addOnFailureListener(e -> {
            // Falla: Comprobar por qué falló
            if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                // El archivo no existe. Es el primer guardado. Creamos uno nuevo.
                Log.d(TAG, "Excel file does not exist. Creating a new one.");
                createNewWorkbookAndUpload(userData, listener);
            } else {
                // Ocurrió un error de red o de otro tipo al descargar.
                Log.e(TAG, "Error downloading Excel file before saving.", e);
                listener.onFailure("Error de red al sincronizar: " + e.getMessage());
            }
        });
    }

    private void modifyAndUploadWorkbook(Map<String, Object> userData, DatabaseManager.DataSaveListener listener) {
        try (FileInputStream fis = new FileInputStream(localFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            // Lógica para encontrar y actualizar la fila o crear una nueva
            updateSheetWithUserData(sheet, userData);

            // Guardar los cambios en el archivo local
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                workbook.write(fos);
            }

            // Subir el archivo modificado
            uploadLocalFile(listener);

        } catch (IOException e) {
            Log.e(TAG, "Error processing existing Excel file.", e);
            listener.onFailure("Error al procesar datos: " + e.getMessage());
        }
    }

    private void createNewWorkbookAndUpload(Map<String, Object> userData, DatabaseManager.DataSaveListener listener) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Usuarios");
            createHeaderRow(sheet); // Crear la fila de cabecera

            // Añadir la primera fila de datos
            updateSheetWithUserData(sheet, userData);

            // Guardar el nuevo workbook en el archivo local
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                workbook.write(fos);
            }

            // Subir el nuevo archivo
            uploadLocalFile(listener);

        } catch (IOException e) {
            Log.e(TAG, "Error creating new Excel workbook.", e);
            listener.onFailure("Error al crear archivo de datos: " + e.getMessage());
        }
    }

    private void updateSheetWithUserData(Sheet sheet, Map<String, Object> userData) {
        String expediente = (String) userData.get("numeroExpediente");
        int rowIndex = findRowIndexByExpediente(sheet, expediente);

        Row row = (rowIndex != -1) ? sheet.getRow(rowIndex) : sheet.createRow(sheet.getLastRowNum() + 1);
        if(row == null) { // Por si la fila existente está corrupta
            row = sheet.createRow(sheet.getLastRowNum() + 1);
        }

        setCellValue(row, COL_EXPEDIENTE, (String) userData.get("numeroExpediente"));
        setCellValue(row, COL_NOMBRE_COMPLETO, (String) userData.get("nombreCompleto"));
        setCellValue(row, COL_NOMBRE, (String) userData.get("nombre"));
        setCellValue(row, COL_APELLIDO_PATERNO, (String) userData.get("apellidoPaterno"));
        setCellValue(row, COL_APELLIDO_MATERNO, (String) userData.get("apellidoMaterno"));
        setCellValue(row, COL_AREA, (String) userData.get("area"));
        setCellValue(row, COL_CATEGORIA, (String) userData.get("categoria"));
        setCellValue(row, COL_CARGO, (String) userData.get("cargo"));
        setCellValue(row, COL_FECHA_INGRESO, (String) userData.get("fechaIngreso"));
        setCellValue(row, COL_HORARIO_ENTRADA, (String) userData.get("horarioEntrada"));
        setCellValue(row, COL_HORARIO_SALIDA, (String) userData.get("horarioSalida"));
        setCellValue(row, COL_TITULAR_TYPE, (String) userData.get("titular"));
        setCellValue(row, COL_ENLACE_ORIGEN, (String) userData.get("enlaceOrigen"));
    }

    private void uploadLocalFile(DatabaseManager.DataSaveListener listener) {
        Uri fileUri = Uri.fromFile(localFile);
        storageReference.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure("Error al subir archivo: " + e.getMessage()));
    }

    private void setCellValue(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(value != null ? value : "");
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        setCellValue(headerRow, COL_EXPEDIENTE, "Número de Expediente");
        setCellValue(headerRow, COL_NOMBRE_COMPLETO, "Nombre Completo");
        setCellValue(headerRow, COL_NOMBRE, "Nombre");
        setCellValue(headerRow, COL_APELLIDO_PATERNO, "Apellido Paterno");
        setCellValue(headerRow, COL_APELLIDO_MATERNO, "Apellido Materno");
        setCellValue(headerRow, COL_AREA, "Área");
        setCellValue(headerRow, COL_CATEGORIA, "Categoría");
        setCellValue(headerRow, COL_CARGO, "Cargo");
        setCellValue(headerRow, COL_FECHA_INGRESO, "Fecha de Ingreso");
        setCellValue(headerRow, COL_HORARIO_ENTRADA, "Horario Entrada");
        setCellValue(headerRow, COL_HORARIO_SALIDA, "Horario Salida");
        setCellValue(headerRow, COL_TITULAR_TYPE, "Tipo de Titular");
        setCellValue(headerRow, COL_ENLACE_ORIGEN, "Enlace Origen");
    }

    private int findRowIndexByExpediente(Sheet sheet, String expediente) {
        if (expediente == null) return -1;
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header
            Cell cell = row.getCell(COL_EXPEDIENTE);
            if (cell != null && getCellStringValue(cell).equalsIgnoreCase(expediente)) {
                return row.getRowNum();
            }
        }
        return -1;
    }
}