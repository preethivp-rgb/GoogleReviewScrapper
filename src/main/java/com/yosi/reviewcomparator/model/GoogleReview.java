package com.yosi.reviewcomparator.model;

public class GoogleReview {
    public String googleReviewId;
    public String reviewerName;
    public Integer starRating;
    public String comment;
    public String relativeTime;

    @Override
    public String toString() {
        return "GoogleReview{reviewer='" + reviewerName + "', rating=" + starRating + "}";
    }
}
