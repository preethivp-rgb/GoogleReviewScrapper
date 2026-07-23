package com.yosi.reviewcomparator.batch;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class BatchResultWriter {

    private static final String[] HEADERS = {
            "practice_id", "google_maps_url", "status",
            "db_review_count", "google_review_count", "counts_match",
            "db_average_rating", "google_rating",
            "matched", "text_mismatch", "rating_mismatch",
            "missing_on_google", "missing_in_db", "error"
    };

    public static void write(String path, List<LocationResult> results) throws Exception {
        File file = new File(path);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Batch Results");

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            int rowIndex = 1;
            for (LocationResult r : results) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(r.practiceId);
                row.createCell(1).setCellValue(r.googleMapsUrl);
                row.createCell(2).setCellValue(r.status);
                row.createCell(3).setCellValue(r.dbReviewCount);
                if (r.googleReviewCount != null) {
                    row.createCell(4).setCellValue(r.googleReviewCount);
                }
                row.createCell(5).setCellValue(r.countsMatch ? "MATCH" : "MISMATCH");
                if (r.dbAverageRating != null) {
                    row.createCell(6).setCellValue(r.dbAverageRating);
                }
                if (r.googleRating != null) {
                    row.createCell(7).setCellValue(r.googleRating);
                }
                row.createCell(8).setCellValue(r.matched);
                row.createCell(9).setCellValue(r.textMismatch);
                row.createCell(10).setCellValue(r.ratingMismatch);
                row.createCell(11).setCellValue(r.missingOnGoogle);
                row.createCell(12).setCellValue(r.missingInDb);
                if (r.errorMessage != null) {
                    row.createCell(13).setCellValue(r.errorMessage);
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
    }
}
