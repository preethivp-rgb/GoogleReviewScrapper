package com.yosi.reviewcomparator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds a NEW tab to the locations workbook (output/Url's.xlsx) for each run,
 * named with the run's date/time, containing practice_id + google_maps_url
 * + the count-comparison results (from output/count_summary.xlsx). Keeps a
 * dated history instead of overwriting a single results tab each run.
 */
public class MergeCountsIntoUrlsSheet {

    private static final String[] HEADERS = {
            "practice_id", "google_maps_url", "status", "db_review_count",
            "google_review_count", "counts_match", "db_average_rating", "google_rating"
    };

    public static void main(String[] args) throws Exception {
        String urlsPath = "./output/Url's.xlsx";
        String countsPath = "./output/count_summary.xlsx";

        Map<Integer, Object[]> resultsByPracticeId = loadCountResults(countsPath);
        System.out.println("Loaded " + resultsByPracticeId.size() + " count result(s).");

        try (FileInputStream in = new FileInputStream(urlsPath);
             Workbook workbook = new XSSFWorkbook(in)) {

            Sheet sourceSheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row sourceHeader = sourceSheet.getRow(0);
            int practiceIdCol = findColumn(sourceHeader, formatter, "practice_id");
            int urlCol = findColumn(sourceHeader, formatter, "maps_uri");
            if (urlCol == -1) {
                urlCol = findColumn(sourceHeader, formatter, "google_maps_url");
            }
            if (practiceIdCol == -1 || urlCol == -1) {
                throw new IllegalStateException("Could not find practice_id / url columns in " + urlsPath);
            }

            String tabName = "Results " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"));
            Sheet resultSheet = workbook.createSheet(sanitizeSheetName(workbook, tabName));

            Row header = resultSheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            int outRow = 1;
            int matched = 0;
            for (int rowIndex = 1; rowIndex <= sourceSheet.getLastRowNum(); rowIndex++) {
                Row sourceRow = sourceSheet.getRow(rowIndex);
                if (sourceRow == null) {
                    continue;
                }

                String practiceIdText = formatter.formatCellValue(sourceRow.getCell(practiceIdCol)).trim();
                String url = formatter.formatCellValue(sourceRow.getCell(urlCol)).trim();
                if (practiceIdText.isEmpty()) {
                    continue;
                }
                int practiceId = Integer.parseInt(practiceIdText);

                Row destRow = resultSheet.createRow(outRow++);
                destRow.createCell(0).setCellValue(practiceId);
                destRow.createCell(1).setCellValue(url);

                Object[] result = resultsByPracticeId.get(practiceId);
                if (result != null) {
                    for (int i = 0; i < result.length; i++) {
                        Cell cell = destRow.createCell(2 + i);
                        if (result[i] == null) {
                            continue;
                        } else if (result[i] instanceof String s) {
                            cell.setCellValue(s);
                        } else if (result[i] instanceof Double d) {
                            cell.setCellValue(d);
                        } else if (result[i] instanceof Integer n) {
                            cell.setCellValue(n);
                        }
                    }
                    matched++;
                }
            }

            System.out.println("Wrote " + matched + " matched row(s) into new tab: " + tabName);

            try (FileOutputStream out = new FileOutputStream(urlsPath)) {
                workbook.write(out);
            }
            System.out.println("Written back to: " + urlsPath);
        }
    }

    private static String sanitizeSheetName(Workbook workbook, String desired) {
        String base = desired.replaceAll("[\\\\/*\\[\\]:?]", "-");
        if (base.length() > 31) {
            base = base.substring(0, 31);
        }
        String name = base;
        int suffix = 2;
        while (workbook.getSheet(name) != null) {
            String suffixStr = " (" + suffix + ")";
            name = base.substring(0, Math.min(base.length(), 31 - suffixStr.length())) + suffixStr;
            suffix++;
        }
        return name;
    }

    private static int findColumn(Row headerRow, DataFormatter formatter, String name) {
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String header = formatter.formatCellValue(headerRow.getCell(c)).trim().toLowerCase();
            if (header.equals(name)) {
                return c;
            }
        }
        return -1;
    }

    private static Map<Integer, Object[]> loadCountResults(String path) throws Exception {
        Map<Integer, Object[]> results = new HashMap<>();

        try (FileInputStream in = new FileInputStream(path);
             Workbook workbook = new XSSFWorkbook(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String practiceIdText = formatter.formatCellValue(row.getCell(0)).trim();
                if (practiceIdText.isEmpty()) {
                    continue;
                }
                int practiceId = Integer.parseInt(practiceIdText);

                String status = formatter.formatCellValue(row.getCell(2)).trim();
                String dbCountText = formatter.formatCellValue(row.getCell(3)).trim();
                String googleCountText = formatter.formatCellValue(row.getCell(4)).trim();
                String countsMatch = formatter.formatCellValue(row.getCell(5)).trim();
                String dbAvgText = formatter.formatCellValue(row.getCell(6)).trim();
                String googleRatingText = formatter.formatCellValue(row.getCell(7)).trim();

                Object[] result = new Object[]{
                        status,
                        parseIntOrNull(dbCountText),
                        parseIntOrNull(googleCountText),
                        countsMatch,
                        parseDoubleOrNull(dbAvgText),
                        parseDoubleOrNull(googleRatingText)
                };

                results.put(practiceId, result);
            }
        }

        return results;
    }

    private static Integer parseIntOrNull(String text) {
        try {
            return text.isEmpty() ? null : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDoubleOrNull(String text) {
        try {
            return text.isEmpty() ? null : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
