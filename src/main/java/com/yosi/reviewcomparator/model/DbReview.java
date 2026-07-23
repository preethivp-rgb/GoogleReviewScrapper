package com.yosi.reviewcomparator.model;

public class DbReview {
    public long id;
    public String source;
    public int practiceId;
    public String socialMediaLocationId;
    public String socialMediaReviewId;
    public String reviewerName;
    public boolean reviewerIsAnonymous;
    public Integer starRating;
    public String comment;
    public String reviewCreatedAt;
    public String reviewUpdatedAt;
    public String replyComment;
    public String replyUpdateTime;

    @Override
    public String toString() {
        return "DbReview{id=" + id + ", reviewer='" + reviewerName + "', rating=" + starRating + "}";
    }
}
