package com.yosi.reviewcomparator.comparator;

import com.yosi.reviewcomparator.model.DbReview;
import com.yosi.reviewcomparator.model.GoogleReview;

public class ComparisonResult {

    public enum Status {
        MATCHED,
        RATING_MISMATCH,
        TEXT_MISMATCH,
        MISSING_IN_DB,
        MISSING_ON_GOOGLE
    }

    public Status status;
    public DbReview dbReview;
    public GoogleReview googleReview;
    public String notes;

    public static ComparisonResult of(Status status, DbReview dbReview, GoogleReview googleReview, String notes) {
        ComparisonResult r = new ComparisonResult();
        r.status = status;
        r.dbReview = dbReview;
        r.googleReview = googleReview;
        r.notes = notes;
        return r;
    }
}
