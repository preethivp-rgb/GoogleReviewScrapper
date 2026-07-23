package com.yosi.reviewcomparator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;

public class DebugReadUrls {
    public static void main(String[] args) throws Exception {
        String path = "./output/Url's.xlsx";

        try (FileInputStream in = new FileInputStream(path);
             Workbook workbook = new XSSFWorkbook(in)) {

            DataFormatter formatter = new DataFormatter();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                System.out.println("=== Sheet: " + sheet.getSheetName() + " ===");

                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    StringBuilder line = new StringBuilder();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        line.append(formatter.formatCellValue(cell)).append(" | ");
                    }
                    System.out.println("Row " + r + ": " + line);
                }
            }
        }
    }
}
