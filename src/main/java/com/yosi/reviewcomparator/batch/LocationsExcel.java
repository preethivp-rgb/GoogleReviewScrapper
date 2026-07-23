package com.yosi.reviewcomparator.batch;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads/writes the batch input file: one row per location, columns
 * practice_id and google_maps_url. Also generates a starter template.
 */
public class LocationsExcel {

    private static final String[] HEADERS = {"practice_id", "google_maps_url"};

    public static void generateTemplateIfMissing(String path) throws Exception {
        File file = new File(path);
        if (file.exists()) {
            return;
        }
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Locations");

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue(808);
            example.createCell(1).setCellValue("https://maps.google.com/maps?cid=13172858372773100288");

            sheet.setColumnWidth(0, 3000);
            sheet.setColumnWidth(1, 12000);

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
    }

    private static final List<String> PRACTICE_ID_HEADER_NAMES = List.of("practice_id", "practiceid");
    private static final List<String> URL_HEADER_NAMES =
            List.of("google_maps_url", "maps_uri", "maps_url", "url", "cid_url");

    public static List<Location> read(String path) throws Exception {
        List<Location> locations = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(path);
             Workbook workbook = new XSSFWorkbook(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return locations;
            }

            int practiceIdCol = -1;
            int urlCol = -1;
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = formatter.formatCellValue(headerRow.getCell(c)).trim().toLowerCase();
                if (PRACTICE_ID_HEADER_NAMES.contains(header)) {
                    practiceIdCol = c;
                } else if (URL_HEADER_NAMES.contains(header)) {
                    urlCol = c;
                }
            }

            if (practiceIdCol == -1 || urlCol == -1) {
                throw new IllegalStateException(
                        "Could not find practice_id / google_maps_url columns in header row of " + path
                                + " (looked for one of " + PRACTICE_ID_HEADER_NAMES
                                + " and one of " + URL_HEADER_NAMES + ")");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String practiceIdText = formatter.formatCellValue(row.getCell(practiceIdCol)).trim();
                String url = formatter.formatCellValue(row.getCell(urlCol)).trim();

                if (practiceIdText.isEmpty() || url.isEmpty()) {
                    continue;
                }

                int practiceId = Integer.parseInt(practiceIdText);
                locations.add(new Location(practiceId, url));
            }
        }

        return locations;
    }
}
