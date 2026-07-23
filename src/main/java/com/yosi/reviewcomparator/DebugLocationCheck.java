package com.yosi.reviewcomparator;

import java.sql.*;

public class DebugLocationCheck {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        String sql = "SELECT DISTINCT social_media_location_id, social_media_review_id FROM "
                + config.reviewsTable() + " WHERE practice_id = ? AND source = 'google' LIMIT 3";

        try (Connection conn = DriverManager.getConnection(config.jdbcUrl(), config.dbUser(), config.dbPassword());
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, config.practiceId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    System.out.println("social_media_location_id=" + rs.getString(1)
                            + " sample_review_id=" + rs.getString(2));
                }
            }
        }
    }
}
