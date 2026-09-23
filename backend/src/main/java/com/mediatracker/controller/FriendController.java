package com.mediatracker.controller;

import com.mediatracker.model.entity.FriendshipEntity;
import com.mediatracker.model.entity.UserFriendPreferenceEntity;
import com.mediatracker.security.SecurityUtils;
import com.mediatracker.service.ActivityService;
import com.mediatracker.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Friends & Activity", description = "Endpoints for friend management, preferences, and social activity feed")
public class FriendController {

    private final FriendshipService friendshipService;
    private final ActivityService activityService;

    public FriendController(FriendshipService friendshipService, ActivityService activityService) {
        this.friendshipService = friendshipService;
        this.activityService = activityService;
    }

    @GetMapping("/friends")
    @Operation(summary = "Get current user's friends and pending friend requests")
    public ResponseEntity<?> getFriends() {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        Map<String, Object> data = friendshipService.getFriendsOverview(currentUserId);
        return ResponseEntity.ok(data);
    }

    public record FriendRequestBody(String target) {}

    @PostMapping("/friends")
    @Operation(summary = "Send a friend request by username or email")
    public ResponseEntity<?> sendFriendRequest(@RequestBody FriendRequestBody body) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        if (body.target() == null || body.target().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "target username or email is required"));
        }

        try {
            FriendshipEntity f = friendshipService.sendFriendRequest(currentUserId, body.target());
            return ResponseEntity.ok(f);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record RespondRequestBody(String action) {}

    @PatchMapping("/friends/{id}")
    @Operation(summary = "Accept, decline, or block a friend request")
    public ResponseEntity<?> respondToFriendRequest(@PathVariable UUID id, @RequestBody RespondRequestBody body) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        boolean success = friendshipService.respondToRequest(currentUserId, id, body.action());
        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update friendship status"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/friends/{id}")
    @Operation(summary = "Remove a friend")
    public ResponseEntity<?> removeFriend(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        boolean removed = friendshipService.removeFriend(currentUserId, id);
        return ResponseEntity.ok(Map.of("success", removed));
    }

    public record PreferenceRequestBody(UUID friendId, Boolean hideActivity, Boolean hideRatings) {}

    @PatchMapping("/friends/preferences")
    @Operation(summary = "Update friend preferences (muting activity or ratings)")
    public ResponseEntity<?> updateFriendPreferences(@RequestBody PreferenceRequestBody body) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        if (body.friendId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "friendId is required"));
        }
        UserFriendPreferenceEntity updated = friendshipService.updatePreferences(
                currentUserId, body.friendId(), body.hideActivity(), body.hideRatings()
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/activity")
    @Operation(summary = "Get activity feed filtered by privacy and muting preferences")
    public ResponseEntity<?> getActivity(
            @RequestParam(defaultValue = "friends") String scope,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null) {
            return ResponseEntity.ok(Map.of("activities", List.of()));
        }

        List<Map<String, Object>> activities = activityService.getActivityFeed(currentUserId, scope, page, limit);
        return ResponseEntity.ok(Map.of("activities", activities));
    }
}
