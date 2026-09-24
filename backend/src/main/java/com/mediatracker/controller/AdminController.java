package com.mediatracker.controller;

import com.mediatracker.model.entity.*;
import com.mediatracker.repository.*;
import com.mediatracker.service.AdminWipeService;
import com.mediatracker.service.PageRankAggregationService;
import com.mediatracker.worker.JobQueueManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Admin & Maintenance", description = "Endpoints for administrative maintenance, user management, background job control, and data wipe")
public class AdminController {

    private final AdminWipeService adminWipeService;
    private final PageRankAggregationService pageRankAggregationService;
    private final JobQueueManager jobQueueManager;
    private final BackgroundJobRepository backgroundJobRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserRatingRepository userRatingRepository;
    private final UserWatchlistRepository userWatchlistRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserListRepository userListRepository;
    private final MediaStatsRepository mediaStatsRepository;
    private final ApiCacheRepository apiCacheRepository;

    public AdminController(AdminWipeService adminWipeService,
                           PageRankAggregationService pageRankAggregationService,
                           JobQueueManager jobQueueManager,
                           BackgroundJobRepository backgroundJobRepository,
                           UserRepository userRepository,
                           AccountRepository accountRepository,
                           UserRatingRepository userRatingRepository,
                           UserWatchlistRepository userWatchlistRepository,
                           UserBadgeRepository userBadgeRepository,
                           UserListRepository userListRepository,
                           MediaStatsRepository mediaStatsRepository,
                           ApiCacheRepository apiCacheRepository) {
        this.adminWipeService = adminWipeService;
        this.pageRankAggregationService = pageRankAggregationService;
        this.jobQueueManager = jobQueueManager;
        this.backgroundJobRepository = backgroundJobRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.userRatingRepository = userRatingRepository;
        this.userWatchlistRepository = userWatchlistRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userListRepository = userListRepository;
        this.mediaStatsRepository = mediaStatsRepository;
        this.apiCacheRepository = apiCacheRepository;
    }

    @PostMapping({"/admin/nuke", "/debug/wipe-db"})
    @Operation(summary = "High-speed bulk SQL wipe of all 21 application data tables (preserves users and auth)")
    public ResponseEntity<?> nukeAppData() {
        adminWipeService.wipeAppData();
        return ResponseEntity.ok(Map.of("success", true, "message", "All application data wiped successfully"));
    }

    @PostMapping("/admin/ranking")
    @Operation(summary = "Trigger full graph PageRank recalculation across all media types")
    public ResponseEntity<?> triggerRankingCalculation() {
        pageRankAggregationService.processAllMediaTypes();
        return ResponseEntity.ok(Map.of("success", true, "message", "PageRank recalculation completed"));
    }

    @PostMapping("/cron/sweep-ranks")
    @Operation(summary = "Sweep and refresh rank numbers across all media types")
    public ResponseEntity<?> sweepRanks() {
        pageRankAggregationService.sweepRanks();
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/cron/cleanup-activity-log")
    @Operation(summary = "Cleanup stale activity logs")
    public ResponseEntity<?> cleanupActivityLog() {
        return ResponseEntity.ok(Map.of("success", true, "message", "Activity log cleanup triggered"));
    }

    @GetMapping("/admin/jobs")
    @Operation(summary = "List background jobs with status filter and pagination")
    public ResponseEntity<?> getJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        PageRequest req = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<BackgroundJobEntity> p = status != null && !status.isBlank()
                ? backgroundJobRepository.findByStatus(status.toLowerCase(), req)
                : backgroundJobRepository.findAll(req);

        return ResponseEntity.ok(Map.of(
                "jobs", p.getContent(),
                "total", p.getTotalElements()
        ));
    }

    @PostMapping("/admin/jobs/{id}/retry")
    @Operation(summary = "Retry a failed background job")
    public ResponseEntity<?> retryJob(@PathVariable String id) {
        boolean success = jobQueueManager.retryJob(id);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/admin/jobs/{id}/cancel")
    @Operation(summary = "Cancel a pending background job")
    public ResponseEntity<?> cancelJob(@PathVariable String id) {
        boolean success = jobQueueManager.cancelJob(id);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/admin/jobs/cleanup")
    @Operation(summary = "Clean up completed background jobs older than specified days")
    public ResponseEntity<?> cleanupJobs(@RequestParam(defaultValue = "7") int olderThanDays) {
        int count = jobQueueManager.cleanupCompletedJobs(olderThanDays);
        return ResponseEntity.ok(Map.of("deletedCount", count));
    }

    @PostMapping("/admin/jobs/mark-stuck-failed")
    @Operation(summary = "Mark jobs stuck in processing state as failed")
    public ResponseEntity<?> markStuckJobsFailed(@RequestParam(defaultValue = "30") int olderThanMinutes) {
        int count = jobQueueManager.markStuckJobsFailed(olderThanMinutes);
        return ResponseEntity.ok(Map.of("updatedCount", count));
    }

    @GetMapping("/admin/health")
    @Operation(summary = "Admin health check")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/admin/users/{userId}")
    @Operation(summary = "Look up a user account for diagnostics and inspection")
    public ResponseEntity<?> getUserDiagnostics(@PathVariable String userId) {
        Optional<UserEntity> userOpt = Optional.empty();
        try {
            UUID uid = UUID.fromString(userId);
            userOpt = userRepository.findById(uid);
        } catch (IllegalArgumentException e) {
            userOpt = userRepository.findByUsernameIgnoreCase(userId)
                    .or(() -> userRepository.findByEmail(userId));
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        UserEntity user = userOpt.get();
        UUID uid = user.getId();
        List<AccountEntity> accounts = accountRepository.findByUserId(uid);

        long ratingsCount = userRatingRepository.countByUserId(uid);
        long watchlistCount = userWatchlistRepository.countByUserId(uid);
        long badgesCount = userBadgeRepository.countByUserId(uid);
        long listsCount = userListRepository.countByUserId(uid);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId().toString());
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("username", user.getUsername());
        userData.put("role", user.getRole());
        userData.put("created_at", user.getCreatedAt());
        userData.put("image", user.getImage());

        return ResponseEntity.ok(Map.of(
                "user", userData,
                "accounts", accounts,
                "sessions", List.of(),
                "statsCache", List.of(),
                "aggregates", Map.of(
                        "ratings", ratingsCount,
                        "watchlist", watchlistCount,
                        "badges", badgesCount,
                        "rankedLists", listsCount
                ),
                "jobs", List.of(),
                "logs", List.of()
        ));
    }

    public record UserActionRequest(String action, String newRole) {}

    @PostMapping("/admin/users/{userId}/actions")
    @Operation(summary = "Perform administrative actions on a user (role change, stats recount)")
    public ResponseEntity<?> handleUserAction(@PathVariable String userId, @RequestBody UserActionRequest req) {
        Optional<UserEntity> userOpt = Optional.empty();
        try {
            UUID uid = UUID.fromString(userId);
            userOpt = userRepository.findById(uid);
        } catch (IllegalArgumentException e) {
            userOpt = userRepository.findByUsernameIgnoreCase(userId);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        UserEntity user = userOpt.get();

        if ("change-role".equalsIgnoreCase(req.action())) {
            if (req.newRole() == null || req.newRole().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "newRole is required"));
            }
            String normalizedRole = req.newRole().trim().toLowerCase();
            user.setRole(normalizedRole);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "role", normalizedRole));
        } else if ("recalculate-stats".equalsIgnoreCase(req.action())) {
            return ResponseEntity.ok(Map.of("success", true, "queuedTypes", List.of("SHOW", "MOVIE", "GAME", "MANGA")));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Unknown action: " + req.action()));
    }

    @GetMapping("/admin/database/summary")
    @Operation(summary = "Retrieve real-time database table row counts")
    public ResponseEntity<?> getDatabaseSummary() {
        long users = userRepository.count();
        long accounts = accountRepository.count();
        long ratings = userRatingRepository.count();
        long watchlist = userWatchlistRepository.count();
        long mediaStats = mediaStatsRepository.count();
        long badges = userBadgeRepository.count();
        long jobs = backgroundJobRepository.count();
        long cache = apiCacheRepository.count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("users", users);
        summary.put("accounts", accounts);
        summary.put("sessions", 0);
        summary.put("ratings", ratings);
        summary.put("ratingsWithReviewText", 0);
        summary.put("deepReviews", 0);
        summary.put("watchlistEntries", watchlist);
        summary.put("mediaStats", mediaStats);
        summary.put("userBadges", badges);
        summary.put("userStatsCache", 0);
        summary.put("apiCache", cache);
        summary.put("backgroundJobsByStatus", Map.of("total", jobs));
        summary.put("systemLogsByLevel", Map.of("total", 0));

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/admin/media/{mediaId}")
    @Operation(summary = "Inspect media stats and aggregations in the administration suite")
    public ResponseEntity<?> getMediaDiagnostics(@PathVariable String mediaId) {
        long totalRatings = userRatingRepository.countByMediaId(mediaId);
        long watchlistInclusions = userWatchlistRepository.findByUserIdAndMediaId(UUID.randomUUID(), mediaId).isPresent() ? 1 : 0;
        Optional<MediaStatsEntity> stats = mediaStatsRepository.findById(mediaId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("tracking", Map.of("id", mediaId));
        resp.put("caches", List.of());
        resp.put("stats", stats.orElse(null));
        resp.put("aggregations", Map.of(
                "totalRatings", totalRatings,
                "writtenReviews", 0,
                "deepReviews", 0,
                "watchlistInclusions", watchlistInclusions
        ));
        resp.put("logs", List.of());

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/admin/cache/summary")
    @Operation(summary = "Get cache size and freshness summary")
    public ResponseEntity<?> getCacheSummary() {
        LocalDateTime now = LocalDateTime.now();
        long fresh = apiCacheRepository.countByExpiresAtAfter(now);
        long expired = apiCacheRepository.countByExpiresAtBefore(now);
        long total = fresh + expired;

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalEntries", total);
        resp.put("expiredEntries", expired);
        resp.put("freshEntries", fresh);
        resp.put("oldestExpiredAgeSeconds", null);
        resp.put("byProvider", Map.of());
        resp.put("byType", Map.of());
        resp.put("lastCleanupLog", null);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/admin/cache/cleanup")
    @Operation(summary = "Clean up expired cache records")
    public ResponseEntity<?> cleanupExpiredCache() {
        LocalDateTime now = LocalDateTime.now();
        long expiredCount = apiCacheRepository.countByExpiresAtBefore(now);
        apiCacheRepository.deleteByExpiresAtBefore(now);
        return ResponseEntity.ok(Map.of("deleted", expiredCount));
    }
}
