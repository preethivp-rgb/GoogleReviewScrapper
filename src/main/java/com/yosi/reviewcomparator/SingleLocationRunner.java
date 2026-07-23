package com.yosi.reviewcomparator;

import com.microsoft.playwright.Page;
import com.yosi.reviewcomparator.batch.LocationResult;
import com.yosi.reviewcomparator.comparator.ComparisonResult;
import com.yosi.reviewcomparator.comparator.ReviewComparator;
import com.yosi.reviewcomparator.db.DbReviewRepository;
import com.yosi.reviewcomparator.model.DbReview;
import com.yosi.reviewcomparator.model.GoogleReview;
import com.yosi.reviewcomparator.report.ReportWriter;
import com.yosi.reviewcomparator.scraper.GoogleReviewScraper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the full DB-vs-Google comparison for a single location and returns a
 * summary. Shared by Main (single-run CLI) and the batch runner (multi-location).
 */
public class SingleLocationRunner {

    /** Standalone run — opens its own browser session for this one location. */
    public static LocationResult run(Config config, String detailReportPath) throws Exception {
        GoogleReviewScraper scraper = new GoogleReviewScraper(config);
        System.out.println("\nScraping Google (single session: aggregate + reviews)...");
        GoogleReviewScraper.ScrapeResult scrapeResult = scraper.scrape();
        return finish(config, detailReportPath, scrapeResult);
    }

    /**
     * Batch run — reuses an already-open page/browser session shared across
     * many locations, instead of launching a new browser per location
     * (launching 100+ times back-to-back on the same profile directory is
     * unreliable, confirmed by repeated testing on the count-only pass).
     */
    public static LocationResult run(Config config, String detailReportPath,
                                      GoogleReviewScraper scraper, Page page) throws Exception {
        System.out.println("\nScraping Google (shared session: aggregate + reviews)...");
        GoogleReviewScraper.ScrapeResult scrapeResult = scraper.scrapeOnPage(page, config.googleMapsUrl());
        return finish(config, detailReportPath, scrapeResult);
    }

    private static LocationResult finish(Config config, String detailReportPath,
                                          GoogleReviewScraper.ScrapeResult scrapeResult) throws Exception {
        System.out.println("Fetching reviews from DB for practice_id=" + config.practiceId() + "...");
        DbReviewRepository dbRepo = new DbReviewRepository(config);
        List<DbReview> dbReviews = dbRepo.fetchReviewsForPractice();
        System.out.println("Found " + dbReviews.size() + " DB review(s).");

        int dbReviewCount = dbRepo.countReviewsForPractice();
        Double dbAverageRating = dbRepo.averageRatingForPractice();

        GoogleReviewScraper.AggregateResult googleStats = scrapeResult.aggregate;
        List<GoogleReview> googleReviews = scrapeResult.reviews;

        System.out.println("\n=== Aggregate check (review count + rating) ===");
        System.out.println("DB review count:     " + dbReviewCount);
        System.out.println("Google review count:  " + googleStats.totalReviewCount);
        System.out.println("DB average rating:    " + dbAverageRating);
        System.out.println("Google rating:         " + googleStats.rating);

        boolean countsMatch = googleStats.totalReviewCount != null
                && googleStats.totalReviewCount.equals(dbReviewCount);
        System.out.println(countsMatch
                ? "Result: MATCH — review counts are equal."
                : "Result: MISMATCH — review counts differ.");

        System.out.println("Found " + googleReviews.size() + " Google review card(s) (target was "
                + googleStats.totalReviewCount + ").");

        System.out.println("Comparing...");
        ReviewComparator comparator = new ReviewComparator();
        List<ComparisonResult> comparisonResults = comparator.compare(dbReviews, googleReviews);

        Map<ComparisonResult.Status, Long> summary = comparisonResults.stream()
                .collect(Collectors.groupingBy(r -> r.status, Collectors.counting()));

        System.out.println("\n=== Summary ===");
        for (ComparisonResult.Status status : ComparisonResult.Status.values()) {
            System.out.println(status + ": " + summary.getOrDefault(status, 0L));
        }

        ReportWriter reportWriter = new ReportWriter();
        reportWriter.writeCsv(detailReportPath, comparisonResults);
        System.out.println("\nDetail report written to: " + detailReportPath);

        LocationResult result = new LocationResult();
        result.practiceId = config.practiceId();
        result.googleMapsUrl = config.googleMapsUrl();
        result.status = "OK";
        result.dbReviewCount = dbReviewCount;
        result.googleReviewCount = googleStats.totalReviewCount;
        result.dbAverageRating = dbAverageRating;
        result.googleRating = googleStats.rating;
        result.countsMatch = countsMatch;
        result.matched = summary.getOrDefault(ComparisonResult.Status.MATCHED, 0L).intValue();
        result.textMismatch = summary.getOrDefault(ComparisonResult.Status.TEXT_MISMATCH, 0L).intValue();
        result.ratingMismatch = summary.getOrDefault(ComparisonResult.Status.RATING_MISMATCH, 0L).intValue();
        result.missingOnGoogle = summary.getOrDefault(ComparisonResult.Status.MISSING_ON_GOOGLE, 0L).intValue();
        result.missingInDb = summary.getOrDefault(ComparisonResult.Status.MISSING_IN_DB, 0L).intValue();

        return result;
    }
}
