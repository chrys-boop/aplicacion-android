package metro.plascreem;

import android.content.Context;
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
    private static final String FILE_NAME = "user_data.xlsx";
    private final File file;

    // Column mapping
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

    public ExcelManager(Context context) {
        file = new File(context.getExternalFilesDir(null), FILE_NAME);
    }

    public Map<String, Object> findUserByExpediente(String expediente) {
        if (!file.exists()) {
            Log.w(TAG, "El archivo de Excel no existe.");
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                Cell expedienteCell = row.getCell(COL_EXPEDIENTE);
                if (expedienteCell != null && expedienteCell.getCellType() == CellType.STRING &&
                        expedienteCell.getStringCellValue().equalsIgnoreCase(expediente)) {
                    return extractUserDataFromRow(row);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al leer el archivo de Excel.", e);
        }
        return null;
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
        userData.put("titularType", getCellStringValue(row.getCell(COL_TITULAR_TYPE)));
        return userData;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        } else {
            return "";
        }
    }

    public boolean saveUserData(Map<String, Object> userData) {
        try {
            Workbook workbook;
            Sheet sheet;
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Usuarios");
                createHeaderRow(sheet);
            }

            String expediente = (String) userData.get("numeroExpediente");
            int rowIndex = findRowIndexByExpediente(sheet, expediente);

            Row row;
            if (rowIndex != -1) {
                row = sheet.getRow(rowIndex);
            } else {
                row = sheet.createRow(sheet.getLastRowNum() + 1);
            }

            if (row == null) {
                row = sheet.createRow(rowIndex != -1 ? rowIndex : sheet.getLastRowNum() + 1);
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
            setCellValue(row, COL_TITULAR_TYPE, (String) userData.get("titularType"));

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            workbook.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error al escribir en el archivo de Excel.", e);
            return false;
        }
    }

    private void setCellValue(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value != null ? value : "");
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(COL_EXPEDIENTE).setCellValue("Número de Expediente");
        headerRow.createCell(COL_NOMBRE_COMPLETO).setCellValue("Nombre Completo");
        headerRow.createCell(COL_NOMBRE).setCellValue("Nombre");
        headerRow.createCell(COL_APELLIDO_PATERNO).setCellValue("Apellido Paterno");
        headerRow.createCell(COL_APELLIDO_MATERNO).setCellValue("Apellido Materno");
        headerRow.createCell(COL_AREA).setCellValue("Área");
        headerRow.createCell(COL_CATEGORIA).setCellValue("Categoría");
        headerRow.createCell(COL_CARGO).setCellValue("Cargo");
        headerRow.createCell(COL_FECHA_INGRESO).setCellValue("Fecha de Ingreso");
        headerRow.createCell(COL_HORARIO_ENTRADA).setCellValue("Horario Entrada");
        headerRow.createCell(COL_HORARIO_SALIDA).setCellValue("Horario Salida");
        headerRow.createCell(COL_TITULAR_TYPE).setCellValue("Tipo de Titular");
    }

    private int findRowIndexByExpediente(Sheet sheet, String expediente) {
        if (expediente == null) return -1;
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header
            Cell cell = row.getCell(COL_EXPEDIENTE);
            if (cell != null && cell.getCellType() == CellType.STRING &&
                    expediente.equalsIgnoreCase(cell.getStringCellValue())) {
                return row.getRowNum();
            }
        }
        return -1;
    }
}
