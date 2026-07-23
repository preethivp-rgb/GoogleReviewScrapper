package com.yosi.reviewcomparator;

import com.yosi.reviewcomparator.batch.BatchResultWriter;
import com.yosi.reviewcomparator.batch.LocationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-off recovery tool: reconstructs LocationResult rows from the batch
 * run's console log, for when a batch was started before incremental
 * writing was added and you want to see progress without waiting for it
 * to finish.
 */
public class ParseLogToPartialResults {

    private static final Pattern LOCATION_HEADER =
            Pattern.compile("Location \\d+/\\d+.*practice_id=(\\d+)");
    private static final Pattern DB_COUNT = Pattern.compile("DB review count:\\s*(\\d+)");
    private static final Pattern GOOGLE_COUNT = Pattern.compile("Google review count:\\s*(\\d+|null)");
    private static final Pattern DB_RATING = Pattern.compile("DB average rating:\\s*([\\d.]+|null)");
    private static final Pattern GOOGLE_RATING = Pattern.compile("Google rating:\\s*([\\d.]+|null)");
    private static final Pattern MATCHED = Pattern.compile("MATCHED:\\s*(\\d+)");
    private static final Pattern TEXT_MISMATCH = Pattern.compile("TEXT_MISMATCH:\\s*(\\d+)");
    private static final Pattern RATING_MISMATCH = Pattern.compile("RATING_MISMATCH:\\s*(\\d+)");
    private static final Pattern MISSING_ON_GOOGLE = Pattern.compile("MISSING_ON_GOOGLE:\\s*(\\d+)");
    private static final Pattern MISSING_IN_DB = Pattern.compile("MISSING_IN_DB:\\s*(\\d+)");
    private static final Pattern ERROR_LINE = Pattern.compile("ERROR processing practice_id=(\\d+): (.*)");

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        String logPath = args.length > 0 ? args[0] : "./output/batch_run.log";
        String log = Files.readString(Path.of(logPath));

        // Split into per-location blocks
        String[] lines = log.split("\n");
        List<LocationResult> results = new ArrayList<>();

        StringBuilder currentBlock = new StringBuilder();
        int currentPracticeId = -1;

        for (String line : lines) {
            Matcher headerMatch = LOCATION_HEADER.matcher(line);
            if (headerMatch.find()) {
                if (currentPracticeId != -1) {
                    LocationResult r = parseBlock(currentPracticeId, currentBlock.toString());
                    if (r != null) {
                        results.add(r);
                    }
                }
                currentPracticeId = Integer.parseInt(headerMatch.group(1));
                currentBlock = new StringBuilder();
                continue;
            }
            currentBlock.append(line).append("\n");
        }
        if (currentPracticeId != -1) {
            LocationResult r = parseBlock(currentPracticeId, currentBlock.toString());
            if (r != null) {
                results.add(r);
            }
        }

        System.out.println("Parsed " + results.size() + " completed location(s) from log.");
        BatchResultWriter.write(config.batchResultsOutputPath(), results);
        System.out.println("Partial results written to: " + config.batchResultsOutputPath());
    }

    private static LocationResult parseBlock(int practiceId, String block) {
        Matcher errorMatch = ERROR_LINE.matcher(block);
        if (errorMatch.find() && Integer.parseInt(errorMatch.group(1)) == practiceId) {
            return LocationResult.failed(practiceId, "", errorMatch.group(2));
        }

        // Only treat as complete if we saw the summary section for this block
        if (!block.contains("=== Summary ===")) {
            return null;
        }

        LocationResult r = new LocationResult();
        r.practiceId = practiceId;
        r.status = "OK";

        r.dbReviewCount = findInt(DB_COUNT, block, 0);
        r.googleReviewCount = findNullableInt(GOOGLE_COUNT, block);
        r.dbAverageRating = findNullableDouble(DB_RATING, block);
        r.googleRating = findNullableDouble(GOOGLE_RATING, block);
        r.countsMatch = r.googleReviewCount != null && r.googleReviewCount.equals(r.dbReviewCount);

        r.matched = findInt(MATCHED, block, 0);
        r.textMismatch = findInt(TEXT_MISMATCH, block, 0);
        r.ratingMismatch = findInt(RATING_MISMATCH, block, 0);
        r.missingOnGoogle = findInt(MISSING_ON_GOOGLE, block, 0);
        r.missingInDb = findInt(MISSING_IN_DB, block, 0);

        return r;
    }

    private static int findInt(Pattern pattern, String text, int fallback) {
        Matcher m = pattern.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : fallback;
    }

    private static Integer findNullableInt(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find() || m.group(1).equals("null")) {
            return null;
        }
        return Integer.parseInt(m.group(1));
    }

    private static Double findNullableDouble(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find() || m.group(1).equals("null")) {
            return null;
        }
        return Double.parseDouble(m.group(1));
    }
}
