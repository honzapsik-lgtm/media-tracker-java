package com.mediatracker.controller;

import com.mediatracker.model.entity.UserRatingEntity;
import com.mediatracker.security.SecurityUtils;
import com.mediatracker.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Ratings & Reviews", description = "Endpoints for submitting, updating, and querying user ratings and deep criteria reviews")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping("/ratings")
    @Operation(summary = "Get user rating for a media item or bulk ratings by prefix")
    public ResponseEntity<?> getRatings(
            @RequestParam(required = false) String mediaId,
            @RequestParam(required = false) String prefix) {

        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);

        if (prefix != null && !prefix.isBlank()) {
            if (currentUserId == null) {
                return ResponseEntity.ok(Map.of("ratings", Map.of()));
            }
            Map<String, Integer> map = ratingService.getPrefixRatings(currentUserId, prefix);
            return ResponseEntity.ok(Map.of("ratings", map));
        }

        if (mediaId == null || mediaId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "mediaId is required"));
        }

        Map<String, Object> data = ratingService.getMediaRatingData(currentUserId, mediaId);
        return ResponseEntity.ok(data);
    }

    public record RatingRequest(
            String mediaId,
            Integer score,
            Boolean isDeepReview,
            Map<String, Object> criteriaScores,
            String reviewText,
            String mediaTitle,
            String mediaImage,
            String mediaReleaseDate
    ) {}

    @PostMapping("/ratings")
    @Operation(summary = "Submit or update rating and deep criteria scores")
    public ResponseEntity<?> saveRating(@RequestBody RatingRequest req) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();

        if (req.mediaId() == null || req.score() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "mediaId and score are required"));
        }

        UserRatingEntity saved = ratingService.saveRating(
                currentUserId,
                req.mediaId(),
                req.score(),
                req.isDeepReview(),
                req.criteriaScores(),
                req.reviewText(),
                req.mediaTitle(),
                req.mediaImage(),
                req.mediaReleaseDate()
        );

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/ratings")
    @Operation(summary = "Delete rating for a media item")
    public ResponseEntity<?> deleteRating(@RequestParam String mediaId) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        boolean deleted = ratingService.deleteRating(currentUserId, mediaId);
        return ResponseEntity.ok(Map.of("success", deleted));
    }

    @GetMapping("/ratings/friends")
    @Operation(summary = "Get ratings from friends for a given media item")
    public ResponseEntity<List<UserRatingEntity>> getFriendRatings(@RequestParam String mediaId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        List<UserRatingEntity> list = ratingService.getFriendRatings(currentUserId, mediaId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get user reviews with text")
    public ResponseEntity<?> getReviews(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        UUID targetUserId = userId != null ? userId : SecurityUtils.getCurrentUserId().orElse(null);
        if (targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        Page<UserRatingEntity> reviewsPage = ratingService.getUserReviews(targetUserId, page, limit);
        return ResponseEntity.ok(Map.of(
                "results", reviewsPage.getContent(),
                "count", reviewsPage.getTotalElements()
        ));
    }
}
