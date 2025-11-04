package metro.plascreem;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

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
    // Apuntamos al archivo y bucket correctos en Supabase
    private static final String BUCKET_NAME = "documentos-MR";
    private static final String FILE_NAME = "abrir.xlsx";
    private final File localFile;
    private final Context context;

    // --- ESTRUCTURA DE COLUMNAS DEFINIDA ---
    private static final int COL_EXPEDIENTE = 0;
    private static final int COL_NOMBRE_COMPLETO = 1;
    private static final int COL_CATEGORIA = 2;
    private static final int COL_AREA = 3;
    private static final int COL_CARGO = 4;
    private static final int COL_FECHA_INGRESO = 5;
    private static final int COL_HORARIO_ENTRADA = 6;
    private static final int COL_HORARIO_SALIDA = 7;
    private static final int COL_TIPO_TITULAR = 8;

    public ExcelManager(Context context) {
        this.context = context;
        this.localFile = new File(context.getCacheDir(), FILE_NAME);
    }

    public interface ExcelDataListener {
        void onDataFound(Map<String, Object> data);
        void onDataNotFound();
        void onError(String message);
    }

    // --- LÓGICA DE BÚSQUEDA ---
    public void findUserByExpediente(String expediente, ExcelDataListener listener) {
        // Usamos el SupabaseManager.kt (ahora corregido) para descargar el archivo
        SupabaseManager.downloadFile(BUCKET_NAME, FILE_NAME, localFile, new SupabaseDownloadListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Excel descargado por SupabaseManager. Leyendo...");
                try (FileInputStream fis = new FileInputStream(localFile);
                     Workbook workbook = new XSSFWorkbook(fis)) {

                    Sheet sheet = workbook.getSheetAt(0);
                    for (Row row : sheet) {
                        if (row.getRowNum() == 0) continue; // Omitir cabecera

                        Cell expedienteCell = row.getCell(COL_EXPEDIENTE);
                        if (expedienteCell != null && getCellStringValue(expedienteCell).equalsIgnoreCase(expediente)) {
                            listener.onDataFound(extractUserDataFromRow(row));
                            return;
                        }
                    }
                    listener.onDataNotFound();

                } catch (IOException e) {
                    Log.e(TAG, "Error leyendo el archivo Excel local.", e);
                    listener.onError("Error de lectura local: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Fallo al descargar desde SupabaseManager: " + message);
                if (message.contains("404")) {
                    listener.onDataNotFound();
                } else {
                    listener.onError("Error de red/Supabase: " + message);
                }
            }
        });
    }

    // --- LÓGICA DE GUARDADO ---
    public void saveUserData(Map<String, Object> userData, DatabaseManager.DataSaveListener listener) {
        SupabaseManager.downloadFile(BUCKET_NAME, FILE_NAME, localFile, new SupabaseDownloadListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Archivo existente descargado. Modificando...");
                modifyAndUploadWorkbook(userData, listener);
            }

            @Override
            public void onFailure(String message) {
                if (message.contains("404")) {
                    Log.d(TAG, "El archivo no existe. Creando uno nuevo...");
                    createNewWorkbookAndUpload(userData, listener);
                } else {
                    listener.onFailure("Error de sincronización: " + message);
                }
            }
        });
    }

    private void modifyAndUploadWorkbook(Map<String, Object> userData, DatabaseManager.DataSaveListener listener) {
        try (FileInputStream fis = new FileInputStream(localFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            updateSheetWithUserData(sheet, userData);

            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                workbook.write(fos);
            }

            uploadLocalFile(listener);

        } catch (IOException e) {
            listener.onFailure("Error procesando Excel: " + e.getMessage());
        }
    }

    private void createNewWorkbookAndUpload(Map<String, Object> userData, DatabaseManager.DataSaveListener listener) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Usuarios");
            createHeaderRow(sheet);
            updateSheetWithUserData(sheet, userData);

            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                workbook.write(fos);
            }

            uploadLocalFile(listener);

        } catch (IOException e) {
            listener.onFailure("Error creando Excel: " + e.getMessage());
        }
    }

    private void uploadLocalFile(DatabaseManager.DataSaveListener listener) {
        Uri fileUri = Uri.fromFile(localFile);
        String storagePath = BUCKET_NAME + "/" + FILE_NAME;

        SupabaseManager.uploadFile(context, fileUri, storagePath, new SupabaseUploadListener() {
            @Override
            public void onSuccess(String publicUrl) {
                Log.d(TAG, "Excel subido con éxito a: " + publicUrl);
                listener.onSuccess();
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Fallo al subir a Supabase: " + message);
                listener.onFailure("Error al guardar en la nube: " + message);
            }
        });
    }

    // --- MÉTODOS DE AYUDA ---

    private void updateSheetWithUserData(Sheet sheet, Map<String, Object> userData) {
        String expediente = (String) userData.get("numeroExpediente");
        int rowIndex = findRowIndexByExpediente(sheet, expediente);

        Row row = (rowIndex != -1) ? sheet.getRow(rowIndex) : sheet.createRow(sheet.getLastRowNum() + 1);
        if(row == null) row = sheet.createRow(sheet.getLastRowNum() + 1);

        setCellValue(row, COL_EXPEDIENTE, (String) userData.get("numeroExpediente"));
        setCellValue(row, COL_NOMBRE_COMPLETO, (String) userData.get("nombreCompleto"));
        setCellValue(row, COL_CATEGORIA, (String) userData.get("categoria"));
        setCellValue(row, COL_AREA, (String) userData.get("area"));
        setCellValue(row, COL_CARGO, (String) userData.get("cargo"));
        setCellValue(row, COL_FECHA_INGRESO, (String) userData.get("fechaIngreso"));
        setCellValue(row, COL_HORARIO_ENTRADA, (String) userData.get("horarioEntrada"));
        setCellValue(row, COL_HORARIO_SALIDA, (String) userData.get("horarioSalida"));
        setCellValue(row, COL_TIPO_TITULAR, (String) userData.get("titular"));
    }

    private Map<String, Object> extractUserDataFromRow(Row row) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("numeroExpediente", getCellStringValue(row.getCell(COL_EXPEDIENTE)));
        userData.put("nombreCompleto", getCellStringValue(row.getCell(COL_NOMBRE_COMPLETO)));
        userData.put("categoria", getCellStringValue(row.getCell(COL_CATEGORIA)));
        userData.put("area", getCellStringValue(row.getCell(COL_AREA)));
        userData.put("cargo", getCellStringValue(row.getCell(COL_CARGO)));
        userData.put("fechaIngreso", getCellStringValue(row.getCell(COL_FECHA_INGRESO)));
        userData.put("horarioEntrada", getCellStringValue(row.getCell(COL_HORARIO_ENTRADA)));
        userData.put("horarioSalida", getCellStringValue(row.getCell(COL_HORARIO_SALIDA)));
        userData.put("titular", getCellStringValue(row.getCell(COL_TIPO_TITULAR)));
        return userData;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private void setCellValue(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(value != null ? value : "");
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        setCellValue(headerRow, COL_EXPEDIENTE, "expediente");
        setCellValue(headerRow, COL_NOMBRE_COMPLETO, "nombre_completo");
        setCellValue(headerRow, COL_CATEGORIA, "categoria");
        setCellValue(headerRow, COL_AREA, "area");
        setCellValue(headerRow, COL_CARGO, "cargo");
        setCellValue(headerRow, COL_FECHA_INGRESO, "fecha_ingreso");
        setCellValue(headerRow, COL_HORARIO_ENTRADA, "horario_entrada");
        setCellValue(headerRow, COL_HORARIO_SALIDA, "horario_salida");
        setCellValue(headerRow, COL_TIPO_TITULAR, "tipo_titular");
    }

    private int findRowIndexByExpediente(Sheet sheet, String expediente) {
        if (expediente == null || expediente.isEmpty()) return -1;
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(COL_EXPEDIENTE);
            if (cell != null && getCellStringValue(cell).equalsIgnoreCase(expediente)) {
                return row.getRowNum();
            }
        }
        return -1;
    }
}
