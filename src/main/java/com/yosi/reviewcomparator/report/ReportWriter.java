package com.yosi.reviewcomparator.report;

import com.opencsv.CSVWriter;
import com.yosi.reviewcomparator.comparator.ComparisonResult;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReportWriter {

    public void writeCsv(String outputPath, List<ComparisonResult> results) throws IOException {
        Path path = Path.of(outputPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
            writer.writeNext(new String[]{
                    "status", "db_id", "reviewer_name", "db_rating", "google_rating",
                    "db_comment", "google_comment", "notes"
            });

            for (ComparisonResult r : results) {
                String reviewer = r.dbReview != null ? r.dbReview.reviewerName
                        : (r.googleReview != null ? r.googleReview.reviewerName : "");
                String dbId = r.dbReview != null ? String.valueOf(r.dbReview.id) : "";
                String dbRating = r.dbReview != null && r.dbReview.starRating != null
                        ? String.valueOf(r.dbReview.starRating) : "";
                String googleRating = r.googleReview != null && r.googleReview.starRating != null
                        ? String.valueOf(r.googleReview.starRating) : "";
                String dbComment = r.dbReview != null ? nullToEmpty(r.dbReview.comment) : "";
                String googleComment = r.googleReview != null ? nullToEmpty(r.googleReview.comment) : "";

                writer.writeNext(new String[]{
                        r.status.name(), dbId, reviewer, dbRating, googleRating,
                        dbComment, googleComment, r.notes
                });
            }
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
