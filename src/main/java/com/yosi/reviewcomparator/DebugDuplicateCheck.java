package com.yosi.reviewcomparator;

import java.sql.*;
import java.util.*;

public class DebugDuplicateCheck {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        String sql = "SELECT id, reviewer_name, comment, social_media_review_id FROM "
                + config.reviewsTable() + " WHERE practice_id = ? AND source = 'google' "
                + "AND comment IS NOT NULL AND comment <> ''";

        Map<String, List<String>> byComment = new LinkedHashMap<>();

        try (Connection conn = DriverManager.getConnection(config.jdbcUrl(), config.dbUser(), config.dbPassword());
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, config.practiceId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String reviewer = rs.getString("reviewer_name");
                    String comment = rs.getString("comment");
                    String reviewId = rs.getString("social_media_review_id");

                    byComment.computeIfAbsent(comment, k -> new ArrayList<>())
                            .add("id=" + id + " reviewer='" + reviewer + "' review_id=" + reviewId);
                }
            }
        }

        int duplicateGroups = 0;
        int duplicateRows = 0;
        for (Map.Entry<String, List<String>> entry : byComment.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateGroups++;
                duplicateRows += entry.getValue().size();
                System.out.println("DUPLICATE COMMENT (x" + entry.getValue().size() + "): "
                        + truncate(entry.getKey(), 80));
                for (String row : entry.getValue()) {
                    System.out.println("    " + row);
                }
            }
        }

        System.out.println("\nTotal distinct comments: " + byComment.size());
        System.out.println("Duplicate comment groups: " + duplicateGroups);
        System.out.println("Total rows involved in duplicates: " + duplicateRows);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
