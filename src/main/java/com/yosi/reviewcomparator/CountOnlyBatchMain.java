package com.yosi.reviewcomparator;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.yosi.reviewcomparator.batch.Location;
import com.yosi.reviewcomparator.batch.LocationsExcel;
import com.yosi.reviewcomparator.db.DbReviewRepository;
import com.yosi.reviewcomparator.scraper.GoogleReviewScraper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Fast pass: for every location, fetch DB count/avg rating and Google's
 * aggregate count/rating only (no scrolling through individual reviews).
 *
 * Uses ONE browser session for the entire batch (navigating to each URL in
 * turn) rather than launching/closing Chrome per location — launching 100+
 * times back-to-back on the same profile directory was unreliable in
 * testing (the previous process doesn't always release its profile lock
 * before the next launch starts, causing intermittent failures regardless
 * of added delays/retries).
 */
public class CountOnlyBatchMain {

    private static final String[] HEADERS = {
            "practice_id", "google_maps_url", "status",
            "db_review_count", "google_review_count", "counts_match",
            "db_average_rating", "google_rating", "error"
    };

    public static void main(String[] args) throws Exception {
        Config baseConfig = new Config();

        List<Location> locations = LocationsExcel.read(baseConfig.locationsInputPath());
        System.out.println("Loaded " + locations.size() + " location(s).");

        String outputPath = "./output/count_summary.xlsx";

        try (Workbook workbook = new XSSFWorkbook();
             Playwright playwright = Playwright.create()) {

            Sheet sheet = workbook.createSheet("Counts");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            GoogleReviewScraper scraper = new GoogleReviewScraper(baseConfig);
            Page page = scraper.openPersistentPage(playwright);

            for (int i = 0; i < locations.size(); i++) {
                Location location = locations.get(i);
                System.out.println("[" + (i + 1) + "/" + locations.size() + "] practice_id=" + location.practiceId);

                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(location.practiceId);
                row.createCell(1).setCellValue(location.googleMapsUrl);

                Config locationConfig = new Config() {
                    @Override
                    public int practiceId() {
                        return location.practiceId;
                    }
                };

                try {
                    DbReviewRepository dbRepo = new DbReviewRepository(locationConfig);
                    int dbCount = dbRepo.countReviewsForPractice();
                    Double dbAvg = dbRepo.averageRatingForPractice();

                    GoogleReviewScraper.AggregateResult stats =
                            scraper.scrapeAggregateOnPage(page, location.googleMapsUrl);

                    boolean countsMatch = stats.totalReviewCount != null
                            && stats.totalReviewCount.equals(dbCount);

                    row.createCell(2).setCellValue("OK");
                    row.createCell(3).setCellValue(dbCount);
                    if (stats.totalReviewCount != null) {
                        row.createCell(4).setCellValue(stats.totalReviewCount);
                    }
                    row.createCell(5).setCellValue(countsMatch ? "MATCH" : "MISMATCH");
                    if (dbAvg != null) {
                        row.createCell(6).setCellValue(dbAvg);
                    }
                    if (stats.rating != null) {
                        row.createCell(7).setCellValue(stats.rating);
                    }

                    System.out.println("  DB=" + dbCount + " Google=" + stats.totalReviewCount
                            + " -> " + (countsMatch ? "MATCH" : "MISMATCH"));
                } catch (Exception e) {
                    row.createCell(2).setCellValue("ERROR");
                    row.createCell(8).setCellValue(e.getMessage());
                    System.out.println("  ERROR: " + e.getMessage());
                }

                // Write after every location so progress is visible live.
                writeWorkbook(workbook, outputPath);
            }

            page.context().close();
        }

        System.out.println("\nCount-only pass complete. Written to: " + outputPath);
    }

    private static void writeWorkbook(Workbook workbook, String path) throws Exception {
        File file = new File(path);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
    }
}
