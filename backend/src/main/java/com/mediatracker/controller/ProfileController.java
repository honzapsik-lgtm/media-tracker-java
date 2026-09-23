package com.mediatracker.controller;

import com.mediatracker.model.entity.UserEntity;
import com.mediatracker.model.entity.UserPrivacySettingsEntity;
import com.mediatracker.model.entity.UserRatingEntity;
import com.mediatracker.model.enums.VisibilityLevel;
import com.mediatracker.security.SecurityUtils;
import com.mediatracker.service.ProfileService;
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
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Endpoints for user profiles, nicknames, showcase badges, and privacy settings")
public class ProfileController {

    private final ProfileService profileService;
    private final RatingService ratingService;

    public ProfileController(ProfileService profileService, RatingService ratingService) {
        this.profileService = profileService;
        this.ratingService = ratingService;
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get user profile with badges and media stats by username")
    public ResponseEntity<?> getProfile(@PathVariable String username) {
        UUID viewerId = SecurityUtils.getCurrentUserId().orElse(null);
        return profileService.getProfile(username, viewerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/username/available")
    @Operation(summary = "Check nickname availability")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        boolean available = profileService.isUsernameAvailable(username, currentUserId);
        return ResponseEntity.ok(Map.of("available", available));
    }

    public record UsernameRequest(String username) {}

    @PostMapping("/username")
    @Operation(summary = "Update current user's nickname")
    public ResponseEntity<?> updateUsername(@RequestBody UsernameRequest body) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        try {
            UserEntity user = profileService.updateUsername(currentUserId, body.username());
            return ResponseEntity.ok(Map.of("success", true, "user", user));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record ProfileUpdateRequest(String realName, String stateRegion, String country, List<String> showcaseBadges) {}

    @PatchMapping
    @Operation(summary = "Update personal profile information and showcase badges")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest body) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        UserEntity user = profileService.updateProfile(
                currentUserId, body.realName(), body.stateRegion(), body.country(), body.showcaseBadges()
        );
        return ResponseEntity.ok(user);
    }

    @GetMapping("/privacy")
    @Operation(summary = "Get current user's privacy settings")
    public ResponseEntity<?> getPrivacy() {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        UserPrivacySettingsEntity privacy = profileService.getPrivacySettings(currentUserId);
        return ResponseEntity.ok(privacy);
    }

    public record PrivacyUpdateRequest(
            VisibilityLevel profileVisibility,
            VisibilityLevel ratingsVisibility,
            VisibilityLevel watchlistVisibility,
            VisibilityLevel activityVisibility
    ) {}

    @PatchMapping("/privacy")
    @Operation(summary = "Update privacy settings")
    public ResponseEntity<?> updatePrivacy(@RequestBody PrivacyUpdateRequest body) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        UserPrivacySettingsEntity updated = profileService.updatePrivacySettings(
                currentUserId, body.profileVisibility(), body.ratingsVisibility(), body.watchlistVisibility(), body.activityVisibility()
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/ratings")
    @Operation(summary = "Get paginated user ratings for profile")
    public ResponseEntity<?> getProfileRatings(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        UUID target = userId != null ? userId : SecurityUtils.getCurrentUserId().orElse(null);
        if (target == null) return ResponseEntity.badRequest().body(Map.of("error", "userId required"));

        Page<UserRatingEntity> p = ratingService.getUserRatings(target, page, limit);
        return ResponseEntity.ok(Map.of("results", p.getContent(), "count", p.getTotalElements()));
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get paginated user reviews for profile")
    public ResponseEntity<?> getProfileReviews(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        UUID target = userId != null ? userId : SecurityUtils.getCurrentUserId().orElse(null);
        if (target == null) return ResponseEntity.badRequest().body(Map.of("error", "userId required"));

        Page<UserRatingEntity> p = ratingService.getUserReviews(target, page, limit);
        return ResponseEntity.ok(Map.of("results", p.getContent(), "count", p.getTotalElements()));
    }

    @GetMapping("/rankings")
    @Operation(summary = "Get paginated user rankings for profile")
    public ResponseEntity<?> getProfileRankings(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "show") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        UUID target = userId != null ? userId : SecurityUtils.getCurrentUserId().orElse(null);
        if (target == null) return ResponseEntity.badRequest().body(Map.of("error", "userId required"));

        Page<UserRatingEntity> p = ratingService.getUserRankings(target, type, page, limit);
        return ResponseEntity.ok(Map.of("results", p.getContent(), "count", p.getTotalElements()));
    }
}
