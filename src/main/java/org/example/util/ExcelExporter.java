package org.example.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.exception.ExportException;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Запись листов в файл Excel (.xlsx) через Apache POI. */
public class ExcelExporter {

    private static final int MAX_COLUMN_WIDTH = 60 * 256;

    /** Стили ячеек создаются один раз на книгу. */
    private static final class Styles {
        private final CellStyle header;
        private final CellStyle money;
        private final CellStyle date;
        private final CellStyle dateTime;

        private Styles(Workbook workbook) {
            Font bold = workbook.createFont();
            bold.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            var format = workbook.createDataFormat();
            money = workbook.createCellStyle();
            money.setDataFormat(format.getFormat("#,##0.00"));
            date = workbook.createCellStyle();
            date.setDataFormat(format.getFormat("dd.mm.yyyy"));
            dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(format.getFormat("dd.mm.yyyy hh:mm"));
        }
    }

    public void export(Path file, List<ExportSheet> sheets) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            for (ExportSheet sheet : sheets) {
                writeSheet(workbook, styles, sheet);
            }
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new ExportException("Не удалось сохранить " + file + ": " + e.getMessage(), e);
        }
    }

    private void writeSheet(Workbook workbook, Styles styles, ExportSheet data) {
        Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(data.name()));
        int columns = data.headers().size();

        Row header = sheet.createRow(0);
        for (int i = 0; i < columns; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(data.headers().get(i));
            cell.setCellStyle(styles.header);
        }

        int rowIndex = 1;
        for (List<Object> values : data.rows()) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < columns; i++) {
                writeCell(row.createCell(i), values.get(i), styles);
            }
        }

        sheet.createFreezePane(0, 1);
        if (!data.rows().isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, data.rows().size(), 0, columns - 1));
        }
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            // запас под кнопку автофильтра
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 3 * 256, MAX_COLUMN_WIDTH));
        }
    }

    private static void writeCell(Cell cell, Object value, Styles styles) {
        switch (value) {
            case null -> cell.setBlank();
            case BigDecimal number -> {
                cell.setCellValue(number.doubleValue());
                cell.setCellStyle(styles.money);
            }
            case Number number -> cell.setCellValue(number.doubleValue());
            case LocalDateTime dateTime -> {
                cell.setCellValue(dateTime);
                cell.setCellStyle(styles.dateTime);
            }
            case LocalDate date -> {
                cell.setCellValue(date);
                cell.setCellStyle(styles.date);
            }
            case Boolean flag -> cell.setCellValue(flag ? "да" : "нет");
            case Enum<?> constant -> cell.setCellValue(constant.name());
            default -> cell.setCellValue(value.toString());
        }
    }
}
