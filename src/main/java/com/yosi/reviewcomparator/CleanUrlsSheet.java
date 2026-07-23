package com.yosi.reviewcomparator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/** One-off: strips any extra result columns from Sheet1, leaving only practice_id/maps_uri. */
public class CleanUrlsSheet {
    public static void main(String[] args) throws Exception {
        String path = "./output/Url's.xlsx";

        try (FileInputStream in = new FileInputStream(path);
             Workbook workbook = new XSSFWorkbook(in)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                // Keep only columns 0 and 1 (practice_id, maps_uri)
                for (int c = row.getLastCellNum() - 1; c >= 2; c--) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        row.removeCell(cell);
                    }
                }
            }

            try (FileOutputStream out = new FileOutputStream(path)) {
                workbook.write(out);
            }
            System.out.println("Cleaned. Sheet1 now has only practice_id/maps_uri columns.");
        }
    }
}
