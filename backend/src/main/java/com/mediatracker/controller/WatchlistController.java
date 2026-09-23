package com.mediatracker.controller;

import com.mediatracker.model.entity.UserWatchlistEntity;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.model.enums.WatchlistStatus;
import com.mediatracker.security.SecurityUtils;
import com.mediatracker.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/watchlist")
@Tag(name = "Watchlist", description = "Endpoints for managing user watchlists and tracking consumption metrics")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    @Operation(summary = "Get user watchlist entries filtered by media type and status")
    public ResponseEntity<?> getWatchlist(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String mediaId,
            @RequestParam(required = false) String media_type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        UUID targetUserId = userId != null ? userId : SecurityUtils.getCurrentUserId().orElse(null);

        if (mediaId != null && !mediaId.isBlank()) {
            if (targetUserId == null) {
                return ResponseEntity.ok(Map.of("status", "none"));
            }
            return watchlistService.getWatchlistItem(targetUserId, mediaId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.ok(Map.of("status", "none")));
        }

        if (targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        MediaType mType = null;
        if (media_type != null && !media_type.isBlank()) {
            try { mType = MediaType.valueOf(media_type.trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        WatchlistStatus wStatus = null;
        if (status != null && !status.isBlank()) {
            try { wStatus = WatchlistStatus.valueOf(status.trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        Page<UserWatchlistEntity> watchlistPage = watchlistService.getUserWatchlist(targetUserId, page, limit, mType, wStatus);
        return ResponseEntity.ok(Map.of(
                "results", watchlistPage.getContent(),
                "count", watchlistPage.getTotalElements()
        ));
    }

    public record WatchlistRequest(
            String mediaId,
            String title,
            String image,
            String mediaType,
            String status,
            Integer episodesWatched,
            Integer chaptersRead,
            Integer volumesRead,
            Double hoursPlayed,
            String platform,
            Integer watchCount
    ) {}

    @PostMapping
    @Operation(summary = "Add or update item in watchlist")
    public ResponseEntity<?> addToWatchlist(@RequestBody WatchlistRequest req) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();

        MediaType mType = null;
        if (req.mediaType() != null && !req.mediaType().isBlank()) {
            try { mType = MediaType.valueOf(req.mediaType().trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        WatchlistStatus wStatus = null;
        if (req.status() != null && !req.status().isBlank()) {
            try { wStatus = WatchlistStatus.valueOf(req.status().trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        UserWatchlistEntity saved = watchlistService.upsertWatchlist(
                currentUserId,
                req.mediaId(),
                req.title(),
                req.image(),
                mType,
                wStatus,
                req.episodesWatched(),
                req.chaptersRead(),
                req.volumesRead(),
                req.hoursPlayed(),
                req.platform(),
                req.watchCount()
        );

        return ResponseEntity.ok(saved);
    }

    @PatchMapping
    @Operation(summary = "Update progress or status of a watchlist item")
    public ResponseEntity<?> updateWatchlistProgress(@RequestBody WatchlistRequest req) {
        return addToWatchlist(req);
    }

    @DeleteMapping
    @Operation(summary = "Remove media from watchlist")
    public ResponseEntity<?> deleteWatchlistItem(@RequestParam String mediaId) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        boolean deleted = watchlistService.deleteWatchlist(currentUserId, mediaId);
        return ResponseEntity.ok(Map.of("success", deleted));
    }
}
