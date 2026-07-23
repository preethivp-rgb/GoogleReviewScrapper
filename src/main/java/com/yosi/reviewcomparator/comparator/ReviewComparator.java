package com.yosi.reviewcomparator.comparator;

import com.yosi.reviewcomparator.model.DbReview;
import com.yosi.reviewcomparator.model.GoogleReview;
import com.yosi.reviewcomparator.util.TextNormalizer;

import java.util.ArrayList;
import java.util.List;

public class ReviewComparator {

    public List<ComparisonResult> compare(List<DbReview> dbReviews, List<GoogleReview> googleReviews) {
        List<ComparisonResult> results = new ArrayList<>();
        boolean[] googleMatched = new boolean[googleReviews.size()];

        for (DbReview dbReview : dbReviews) {
            int matchIndex = findMatch(dbReview, googleReviews, googleMatched);

            if (matchIndex == -1) {
                results.add(ComparisonResult.of(
                        ComparisonResult.Status.MISSING_ON_GOOGLE, dbReview, null,
                        "Review exists in DB but no matching reviewer/comment found on Google"));
                continue;
            }

            googleMatched[matchIndex] = true;
            GoogleReview googleReview = googleReviews.get(matchIndex);

            if (dbReview.starRating != null && googleReview.starRating != null
                    && !dbReview.starRating.equals(googleReview.starRating)) {
                results.add(ComparisonResult.of(
                        ComparisonResult.Status.RATING_MISMATCH, dbReview, googleReview,
                        "DB rating=" + dbReview.starRating + " vs Google rating=" + googleReview.starRating));
                continue;
            }

            String normalizedDbComment = TextNormalizer.normalize(dbReview.comment);
            String normalizedGoogleComment = TextNormalizer.normalize(googleReview.comment);
            if (!normalizedDbComment.equals(normalizedGoogleComment)
                    && !normalizedGoogleComment.isEmpty()) {
                results.add(ComparisonResult.of(
                        ComparisonResult.Status.TEXT_MISMATCH, dbReview, googleReview,
                        "Comment text differs after normalization"));
                continue;
            }

            results.add(ComparisonResult.of(ComparisonResult.Status.MATCHED, dbReview, googleReview, ""));
        }

        for (int i = 0; i < googleReviews.size(); i++) {
            if (!googleMatched[i]) {
                results.add(ComparisonResult.of(
                        ComparisonResult.Status.MISSING_IN_DB, null, googleReviews.get(i),
                        "Review found on Google but not present in DB"));
            }
        }

        return results;
    }

    private int findMatch(DbReview dbReview, List<GoogleReview> googleReviews, boolean[] alreadyMatched) {
        // Prefer exact review ID matching — Google's API returns the same ID format
        // stored in social_media_review_id.
        if (dbReview.socialMediaReviewId != null && !dbReview.socialMediaReviewId.isBlank()) {
            for (int i = 0; i < googleReviews.size(); i++) {
                if (alreadyMatched[i]) {
                    continue;
                }
                if (dbReview.socialMediaReviewId.equals(googleReviews.get(i).googleReviewId)) {
                    return i;
                }
            }
        }

        String dbName = TextNormalizer.normalizeName(dbReview.reviewerName);
        String dbComment = TextNormalizer.normalize(dbReview.comment);

        for (int i = 0; i < googleReviews.size(); i++) {
            if (alreadyMatched[i]) {
                continue;
            }
            GoogleReview candidate = googleReviews.get(i);
            String candidateName = TextNormalizer.normalizeName(candidate.reviewerName);

            if (!dbName.equals(candidateName)) {
                continue;
            }

            String candidateComment = TextNormalizer.normalize(candidate.comment);
            if (dbComment.isEmpty() || candidateComment.startsWith(safeSubstring(dbComment, 50))
                    || dbComment.startsWith(safeSubstring(candidateComment, 50))) {
                return i;
            }
        }

        // Fall back to name-only match when comment can't be reliably compared
        // (e.g. Google truncated the text differently or DB comment is null)
        for (int i = 0; i < googleReviews.size(); i++) {
            if (alreadyMatched[i]) {
                continue;
            }
            if (dbName.equals(TextNormalizer.normalizeName(googleReviews.get(i).reviewerName))) {
                return i;
            }
        }

        return -1;
    }

    private String safeSubstring(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
