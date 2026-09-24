package com.mediatracker.controller;

import com.mediatracker.model.entity.*;
import com.mediatracker.repository.*;
import com.mediatracker.service.AdminWipeService;
import com.mediatracker.service.AdminDiagnosticsService;
import com.mediatracker.service.PageRankAggregationService;
import com.mediatracker.worker.JobQueueManager;
import com.mediatracker.worker.JobScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final JobScheduler jobScheduler;
    private final BackgroundJobRepository backgroundJobRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserRatingRepository userRatingRepository;
    private final UserWatchlistRepository userWatchlistRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserListRepository userListRepository;
    private final MediaStatsRepository mediaStatsRepository;
    private final ApiCacheRepository apiCacheRepository;
    private final AdminDiagnosticsService diagnosticsService;

    public AdminController(AdminWipeService adminWipeService,
                           PageRankAggregationService pageRankAggregationService,
                           JobQueueManager jobQueueManager,
                           JobScheduler jobScheduler,
                           BackgroundJobRepository backgroundJobRepository,
                           UserRepository userRepository,
                           AccountRepository accountRepository,
                           UserRatingRepository userRatingRepository,
                           UserWatchlistRepository userWatchlistRepository,
                           UserBadgeRepository userBadgeRepository,
                           UserListRepository userListRepository,
                           MediaStatsRepository mediaStatsRepository,
                           ApiCacheRepository apiCacheRepository,
                           AdminDiagnosticsService diagnosticsService) {
        this.adminWipeService = adminWipeService;
        this.pageRankAggregationService = pageRankAggregationService;
        this.jobQueueManager = jobQueueManager;
        this.jobScheduler = jobScheduler;
        this.backgroundJobRepository = backgroundJobRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.userRatingRepository = userRatingRepository;
        this.userWatchlistRepository = userWatchlistRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userListRepository = userListRepository;
        this.mediaStatsRepository = mediaStatsRepository;
        this.apiCacheRepository = apiCacheRepository;
        this.diagnosticsService = diagnosticsService;
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sweep and refresh rank numbers across all media types")
    public ResponseEntity<?> sweepRanks() {
        pageRankAggregationService.sweepRanks();
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/admin/jobs/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process a bounded batch of due background jobs")
    public ResponseEntity<?> processJobs(@RequestParam(defaultValue = "10") int batchSize) {
        if (batchSize < 1 || batchSize > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "batchSize must be between 1 and 100"));
        }
        return ResponseEntity.ok(jobScheduler.processBatch(batchSize));
    }

    @GetMapping("/admin/jobs/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getJobSummary() {
        return ResponseEntity.ok(jobQueueManager.getSummary());
    }

    @GetMapping("/admin/jobs")
    @Operation(summary = "List background jobs with combined filters and pagination")
    public ResponseEntity<?> getJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String dedupeKey,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        if (page < 1 || limit < 1 || limit > 100 || (long) (page - 1) * limit > Integer.MAX_VALUE) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid pagination: page must be positive, limit 1-100, and offset within integer range"));
        }
        String normalizedStatus = jobFilter(status);
        if (normalizedStatus != null) normalizedStatus = normalizedStatus.toLowerCase(Locale.ROOT);
        Page<BackgroundJobEntity> p = backgroundJobRepository.findAdminJobs(normalizedStatus,
                jobFilter(type), jobFilter(dedupeKey), jobFilter(userId), jobFilter(q), PageRequest.of(page - 1, limit));

        return ResponseEntity.ok(Map.of(
                "jobs", p.getContent(),
                "total", p.getTotalElements()
        ));
    }

    private static String jobFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
        return diagnosticsService.userDiagnostics(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
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
            List<String> queuedTypes = new ArrayList<>();
            for (var mediaType : com.mediatracker.model.enums.MediaType.values()) {
                jobQueueManager.enqueueJob("update_user_stats",
                        Map.of("userId", user.getId().toString(), "mediaType", mediaType.name()),
                        "update_user_stats:" + user.getId() + ":" + mediaType.name(), null, null);
                queuedTypes.add(mediaType.name());
            }
            return ResponseEntity.ok(Map.of("success", true, "queuedTypes", queuedTypes));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Unknown action: " + req.action()));
    }

    @GetMapping("/admin/database/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retrieve real-time database table row counts")
    public ResponseEntity<?> getDatabaseSummary() {
        return ResponseEntity.ok(diagnosticsService.databaseSummary());
    }

    @GetMapping("/admin/media/{mediaId}")
    @Operation(summary = "Inspect media stats and aggregations in the administration suite")
    public ResponseEntity<?> getMediaDiagnostics(@PathVariable String mediaId) {
        return ResponseEntity.ok(diagnosticsService.mediaDiagnostics(mediaId));
    }

    @GetMapping("/admin/cache/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get cache size and freshness summary")
    public ResponseEntity<?> getCacheSummary() {
        return ResponseEntity.ok(diagnosticsService.cacheSummary());
    }

    @PostMapping("/admin/cache/cleanup")
    @Operation(summary = "Clean up expired cache records")
    public ResponseEntity<?> cleanupExpiredCache() {
        LocalDateTime now = LocalDateTime.now();
        long expiredCount = apiCacheRepository.countByExpiresAtBefore(now);
        apiCacheRepository.deleteByExpiresAtBefore(now);
        return ResponseEntity.ok(Map.of("deleted", expiredCount));
    }

    @PostMapping("/admin/cache/flush-all")
    @Operation(summary = "Flush all cache records")
    public ResponseEntity<?> flushAllCache() {
        long count = apiCacheRepository.count();
        apiCacheRepository.deleteAllInBatch();
        return ResponseEntity.ok(Map.of("deleted", count));
    }
}
