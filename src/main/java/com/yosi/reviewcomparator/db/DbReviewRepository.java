package com.yosi.reviewcomparator.db;

import com.yosi.reviewcomparator.Config;
import com.yosi.reviewcomparator.model.DbReview;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbReviewRepository {

    private final Config config;

    public DbReviewRepository(Config config) {
        this.config = config;
    }

    public List<DbReview> fetchReviewsForPractice() throws SQLException {
        String sql = "SELECT id, source, practice_id, social_media_location_id, social_media_review_id, "
                + "reviewer_name, reviewer_is_anonymous, star_rating, comment, "
                + "review_created_at, review_updated_at, reply_comment, reply_update_time "
                + "FROM " + config.reviewsTable() + " "
                + "WHERE practice_id = ? AND source = 'google'";

        List<DbReview> reviews = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(
                config.jdbcUrl(), config.dbUser(), config.dbPassword());
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, config.practiceId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DbReview review = new DbReview();
                    review.id = rs.getLong("id");
                    review.source = rs.getString("source");
                    review.practiceId = rs.getInt("practice_id");
                    review.socialMediaLocationId = rs.getString("social_media_location_id");
                    review.socialMediaReviewId = rs.getString("social_media_review_id");
                    review.reviewerName = rs.getString("reviewer_name");
                    review.reviewerIsAnonymous = rs.getBoolean("reviewer_is_anonymous");
                    int rating = rs.getInt("star_rating");
                    review.starRating = rs.wasNull() ? null : rating;
                    review.comment = rs.getString("comment");
                    review.reviewCreatedAt = rs.getString("review_created_at");
                    review.reviewUpdatedAt = rs.getString("review_updated_at");
                    review.replyComment = rs.getString("reply_comment");
                    review.replyUpdateTime = rs.getString("reply_update_time");
                    reviews.add(review);
                }
            }
        }

        return reviews;
    }

    public int countReviewsForPractice() throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + config.reviewsTable()
                + " WHERE practice_id = ? AND source = 'google'";

        try (Connection conn = DriverManager.getConnection(
                config.jdbcUrl(), config.dbUser(), config.dbPassword());
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, config.practiceId());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Double averageRatingForPractice() throws SQLException {
        String sql = "SELECT AVG(star_rating) FROM " + config.reviewsTable()
                + " WHERE practice_id = ? AND source = 'google'";

        try (Connection conn = DriverManager.getConnection(
                config.jdbcUrl(), config.dbUser(), config.dbPassword());
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, config.practiceId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? null : avg;
                }
                return null;
            }
        }
    }
}
