package com.mediatracker.controller;

import com.mediatracker.model.entity.BackgroundJobEntity;
import com.mediatracker.repository.BackgroundJobRepository;
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

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Admin & Maintenance", description = "Endpoints for administrative maintenance, background job control, and data wipe")
public class AdminController {

    private final AdminWipeService adminWipeService;
    private final PageRankAggregationService pageRankAggregationService;
    private final JobQueueManager jobQueueManager;
    private final BackgroundJobRepository backgroundJobRepository;

    public AdminController(AdminWipeService adminWipeService,
                           PageRankAggregationService pageRankAggregationService,
                           JobQueueManager jobQueueManager,
                           BackgroundJobRepository backgroundJobRepository) {
        this.adminWipeService = adminWipeService;
        this.pageRankAggregationService = pageRankAggregationService;
        this.jobQueueManager = jobQueueManager;
        this.backgroundJobRepository = backgroundJobRepository;
    }

    @PostMapping("/admin/nuke")
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
}
