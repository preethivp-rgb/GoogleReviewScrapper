package com.yosi.reviewcomparator;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    private final Dotenv dotenv;

    public Config() {
        this.dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    public String dbType() {
        return get("DB_TYPE", "mysql");
    }

    public String dbHost() {
        return require("DB_HOST");
    }

    public int dbPort() {
        return Integer.parseInt(get("DB_PORT", "3306"));
    }

    public String dbName() {
        return require("DB_NAME");
    }

    public String dbUser() {
        return require("DB_USER");
    }

    public String dbPassword() {
        return require("DB_PASSWORD");
    }

    public String dbJdbcUrlOverride() {
        return get("DB_JDBC_URL", "");
    }

    public String jdbcUrl() {
        String override = dbJdbcUrlOverride();
        if (override != null && !override.isBlank()) {
            return override;
        }
        return "jdbc:mysql://" + dbHost() + ":" + dbPort() + "/" + dbName()
                + "?useSSL=true&serverTimezone=UTC";
    }

    public String reviewsTable() {
        return get("DB_SOCIAL_MEDIA_REVIEWS_TABLE", "social_media_reviews");
    }

    public int practiceId() {
        return Integer.parseInt(require("PRACTICE_ID"));
    }

    public String googleMapsUrl() {
        return require("GOOGLE_MAPS_URL");
    }

    public boolean playwrightHeadless() {
        return Boolean.parseBoolean(get("PLAYWRIGHT_HEADLESS", "true"));
    }

    public int playwrightTimeoutMs() {
        return Integer.parseInt(get("PLAYWRIGHT_TIMEOUT_MS", "30000"));
    }

    public String reportOutputPath() {
        return get("REPORT_OUTPUT_PATH", "./output/comparison_report.csv");
    }

    public String googlePlacesApiKey() {
        return require("GOOGLE_PLACES_API_KEY");
    }

    public String googlePlaceId() {
        return get("GOOGLE_PLACE_ID", "");
    }

    public String businessName() {
        return get("BUSINESS_NAME", "");
    }

    public String chromeProfileDir() {
        return require("CHROME_PROFILE_DIR");
    }

    public String googleOAuthClientId() {
        return require("GOOGLE_OAUTH_CLIENT_ID");
    }

    public String googleOAuthClientSecret() {
        return require("GOOGLE_OAUTH_CLIENT_SECRET");
    }

    public String googleAccountId() {
        return get("GOOGLE_ACCOUNT_ID", "");
    }

    public String googleLocationId() {
        return get("GOOGLE_LOCATION_ID", "");
    }

    public String googleOAuthTokenStorePath() {
        return get("GOOGLE_OAUTH_TOKEN_STORE_PATH", "./output/google-oauth-tokens.json");
    }

    public String sortBy() {
        return get("SORT_BY", "newest");
    }

    public int maxReviews() {
        return Integer.parseInt(get("MAX_REVIEWS", "0"));
    }

    public int maxScrollAttempts() {
        return Integer.parseInt(get("MAX_SCROLL_ATTEMPTS", "50"));
    }

    public int scrollIdleLimit() {
        return Integer.parseInt(get("SCROLL_IDLE_LIMIT", "15"));
    }

    public String locationsInputPath() {
        return get("LOCATIONS_INPUT_PATH", "./input/locations.xlsx");
    }

    public String batchResultsOutputPath() {
        return get("BATCH_RESULTS_OUTPUT_PATH", "./output/batch_results.xlsx");
    }

    public String batchDetailReportDir() {
        return get("BATCH_DETAIL_REPORT_DIR", "./output/batch_detail");
    }

    private String get(String key, String fallback) {
        String value = dotenv.get(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String require(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required .env value: " + key);
        }
        return value;
    }
}
