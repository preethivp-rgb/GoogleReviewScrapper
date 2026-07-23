package com.yosi.reviewcomparator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;

public class DebugCheckUrlsHeaders {
    public static void main(String[] args) throws Exception {
        try (FileInputStream in = new FileInputStream("./output/Url's.xlsx");
             Workbook wb = new XSSFWorkbook(in)) {
            Row header = wb.getSheetAt(0).getRow(0);
            DataFormatter f = new DataFormatter();
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                sb.append(f.formatCellValue(header.getCell(c))).append(" | ");
            }
            System.out.println(sb);
        }
    }
}
