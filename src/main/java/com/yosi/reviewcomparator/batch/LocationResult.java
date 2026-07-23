package com.yosi.reviewcomparator.batch;

public class LocationResult {
    public int practiceId;
    public String googleMapsUrl;
    public String status;
    public String errorMessage;

    public int dbReviewCount;
    public Integer googleReviewCount;
    public Double dbAverageRating;
    public Double googleRating;
    public boolean countsMatch;

    public int matched;
    public int textMismatch;
    public int ratingMismatch;
    public int missingOnGoogle;
    public int missingInDb;

    public static LocationResult failed(int practiceId, String url, String errorMessage) {
        LocationResult r = new LocationResult();
        r.practiceId = practiceId;
        r.googleMapsUrl = url;
        r.status = "ERROR";
        r.errorMessage = errorMessage;
        return r;
    }
}
