package com.yosi.reviewcomparator;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.yosi.reviewcomparator.batch.BatchResultWriter;
import com.yosi.reviewcomparator.batch.Location;
import com.yosi.reviewcomparator.batch.LocationResult;
import com.yosi.reviewcomparator.batch.LocationsExcel;
import com.yosi.reviewcomparator.scraper.GoogleReviewScraper;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch entry point: reads practice_id + google_maps_url pairs from an Excel
 * file (see LOCATIONS_INPUT_PATH), runs the full comparison for each, and
 * writes a consolidated summary Excel plus a per-location detail CSV.
 *
 * Uses ONE browser session for the entire batch (see SingleLocationRunner's
 * shared-session overload) rather than launching/closing Chrome per
 * location — launching 100+ times back-to-back on the same profile
 * directory was unreliable in testing.
 */
public class BatchMain {

    public static void main(String[] args) throws Exception {
        Config baseConfig = new Config();

        String locationsPath = baseConfig.locationsInputPath();
        LocationsExcel.generateTemplateIfMissing(locationsPath);

        List<Location> locations = LocationsExcel.read(locationsPath);
        if (locations.isEmpty()) {
            System.out.println("No locations found in " + locationsPath
                    + " — fill in practice_id/google_maps_url rows and re-run.");
            return;
        }

        System.out.println("Loaded " + locations.size() + " location(s) from " + locationsPath);

        List<LocationResult> results = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            GoogleReviewScraper scraper = new GoogleReviewScraper(baseConfig);
            Page page = scraper.openPersistentPage(playwright);

            for (int i = 0; i < locations.size(); i++) {
                Location location = locations.get(i);
                System.out.println("\n================================================");
                System.out.println("Location " + (i + 1) + "/" + locations.size()
                        + " — practice_id=" + location.practiceId);
                System.out.println("================================================");

                Config locationConfig = new Config() {
                    @Override
                    public int practiceId() {
                        return location.practiceId;
                    }

                    @Override
                    public String googleMapsUrl() {
                        return location.googleMapsUrl;
                    }
                };

                String detailReportPath = baseConfig.batchDetailReportDir()
                        + "/practice_" + location.practiceId + ".csv";

                try {
                    LocationResult result = SingleLocationRunner.run(locationConfig, detailReportPath, scraper, page);
                    results.add(result);
                } catch (Exception e) {
                    System.out.println("ERROR processing practice_id=" + location.practiceId + ": " + e.getMessage());
                    results.add(LocationResult.failed(location.practiceId, location.googleMapsUrl, e.getMessage()));
                }

                // Write after every location, not just at the end, so progress
                // is visible in the sheet while a long batch is still running.
                BatchResultWriter.write(baseConfig.batchResultsOutputPath(), results);
            }

            page.context().close();
        }

        System.out.println("\n\nBatch complete. Summary written to: " + baseConfig.batchResultsOutputPath());
    }
}
