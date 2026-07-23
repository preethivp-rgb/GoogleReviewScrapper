package com.yosi.reviewcomparator.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yosi.reviewcomparator.Config;
import com.yosi.reviewcomparator.model.GoogleReview;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses the Places API (New) to fetch reviews for a place.
 * NOTE: Places API returns at most 5 "most relevant" reviews per place — it is
 * not a full export of every review, unlike the (now partner-restricted) My Business API.
 */
public class GooglePlacesReviewClient {

    private static final String BASE_URL = "https://places.googleapis.com/v1";

    private final Config config;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GooglePlacesReviewClient(Config config) {
        this.config = config;
    }

    public static class AggregateStats {
        public Double rating;
        public Integer userRatingCount;
    }

    public AggregateStats fetchAggregateStats() throws Exception {
        String placeId = config.googlePlaceId();
        if (placeId == null || placeId.isBlank()) {
            placeId = resolvePlaceId(config.businessName());
        }

        String url = BASE_URL + "/places/" + placeId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Goog-Api-Key", config.googlePlacesApiKey())
                .header("X-Goog-FieldMask", "rating,userRatingCount")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Places API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        AggregateStats stats = new AggregateStats();
        stats.rating = root.has("rating") ? root.get("rating").asDouble() : null;
        stats.userRatingCount = root.has("userRatingCount") ? root.get("userRatingCount").asInt() : null;
        return stats;
    }

    public List<GoogleReview> fetchReviews() throws Exception {
        String placeId = config.googlePlaceId();
        if (placeId == null || placeId.isBlank()) {
            placeId = resolvePlaceId(config.businessName());
        }

        String url = BASE_URL + "/places/" + placeId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Goog-Api-Key", config.googlePlacesApiKey())
                .header("X-Goog-FieldMask", "id,displayName,rating,userRatingCount,reviews")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Places API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        List<GoogleReview> reviews = new ArrayList<>();

        JsonNode reviewsNode = root.get("reviews");
        if (reviewsNode != null && reviewsNode.isArray()) {
            for (JsonNode reviewNode : reviewsNode) {
                reviews.add(toGoogleReview(reviewNode));
            }
        }

        return reviews;
    }

    private GoogleReview toGoogleReview(JsonNode reviewNode) {
        GoogleReview review = new GoogleReview();

        JsonNode author = reviewNode.get("authorAttribution");
        review.reviewerName = author != null && author.has("displayName")
                ? author.get("displayName").asText() : null;

        review.starRating = reviewNode.has("rating") ? reviewNode.get("rating").asInt() : null;

        JsonNode text = reviewNode.get("text");
        review.comment = text != null && text.has("text") ? text.get("text").asText() : null;

        review.relativeTime = reviewNode.has("publishTime") ? reviewNode.get("publishTime").asText() : null;

        // Places API does not expose a stable review ID comparable to social_media_review_id.
        review.googleReviewId = null;

        return review;
    }

    private String resolvePlaceId(String businessName) throws Exception {
        if (businessName == null || businessName.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_PLACE_ID is blank and BUSINESS_NAME is not set — cannot resolve a place ID.");
        }

        String url = BASE_URL + "/places:searchText";
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("textQuery", businessName));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", config.googlePlacesApiKey())
                .header("X-Goog-FieldMask", "places.id,places.displayName")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Places API text search error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode places = root.get("places");
        if (places == null || !places.isArray() || places.isEmpty()) {
            throw new RuntimeException("No place found for business name: " + businessName);
        }

        return places.get(0).get("id").asText();
    }
}
