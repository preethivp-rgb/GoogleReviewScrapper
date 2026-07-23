package com.yosi.reviewcomparator.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.yosi.reviewcomparator.Config;
import com.yosi.reviewcomparator.model.GoogleReview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads reviews via the legacy My Business API v4 reviews endpoint, which is
 * still the only Google API surface that exposes individual review text/rating/author.
 */
public class GoogleBusinessReviewClient {

    private static final String BASE_URL = "https://mybusiness.googleapis.com/v4";

    private final Config config;
    private final Credential credential;

    public GoogleBusinessReviewClient(Config config, Credential credential) {
        this.config = config;
        this.credential = credential;
    }

    public List<GoogleReview> fetchReviews() throws Exception {
        List<GoogleReview> reviews = new ArrayList<>();
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(credential);

        String pageToken = null;
        do {
            String url = BASE_URL + "/accounts/" + config.googleAccountId()
                    + "/locations/" + config.googleLocationId() + "/reviews"
                    + (pageToken != null ? "?pageToken=" + pageToken : "");

            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(url));
            request.setParser(new JsonObjectParser(new JacksonFactory()));
            HttpResponse response = request.execute();

            Map<String, Object> body = response.parseAs(Map.class);
            Object reviewsObj = body.get("reviews");
            if (reviewsObj instanceof List<?> reviewList) {
                for (Object item : reviewList) {
                    if (item instanceof Map<?, ?> reviewMap) {
                        reviews.add(toGoogleReview(reviewMap));
                    }
                }
            }

            Object nextToken = body.get("nextPageToken");
            pageToken = nextToken != null ? nextToken.toString() : null;
        } while (pageToken != null);

        return reviews;
    }

    @SuppressWarnings("unchecked")
    private GoogleReview toGoogleReview(Map<?, ?> reviewMap) {
        GoogleReview review = new GoogleReview();

        // reviewId format matches DB's social_media_review_id, e.g. "AbFvOqmm..."
        Object reviewId = reviewMap.get("reviewId");
        review.comment = (String) reviewMap.get("comment");

        Object reviewer = reviewMap.get("reviewer");
        if (reviewer instanceof Map<?, ?> reviewerMap) {
            review.reviewerName = (String) reviewerMap.get("displayName");
        }

        Object starRating = reviewMap.get("starRating");
        review.starRating = mapStarRating((String) starRating);

        review.relativeTime = (String) reviewMap.get("createTime");

        // Stash the review ID for exact matching against DB's social_media_review_id.
        review.googleReviewId = reviewId != null ? reviewId.toString() : null;

        return review;
    }

    private Integer mapStarRating(String rating) {
        if (rating == null) {
            return null;
        }
        return switch (rating) {
            case "ONE" -> 1;
            case "TWO" -> 2;
            case "THREE" -> 3;
            case "FOUR" -> 4;
            case "FIVE" -> 5;
            default -> null;
        };
    }
}
