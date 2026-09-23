package com.mediatracker.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.mediatracker.model.entity.BackgroundJobEntity;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.service.PageRankAggregationService;
import com.mediatracker.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);
    private final String workerId = "worker_" + UUID.randomUUID();

    private final JobQueueManager jobQueueManager;
    private final StatsService statsService;
    private final PageRankAggregationService pageRankAggregationService;

    public JobScheduler(JobQueueManager jobQueueManager,
                        StatsService statsService,
                        PageRankAggregationService pageRankAggregationService) {
        this.jobQueueManager = jobQueueManager;
        this.statsService = statsService;
        this.pageRankAggregationService = pageRankAggregationService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processNextJob() {
        Optional<BackgroundJobEntity> jobOpt = jobQueueManager.claimJob(workerId);
        if (jobOpt.isEmpty()) return;

        BackgroundJobEntity job = jobOpt.get();
        log.info("Worker {} picked up job {} [{}]", workerId, job.getId(), job.getType());

        try {
            executeJob(job);
            jobQueueManager.completeJob(job.getId());
        } catch (Exception e) {
            log.error("Worker {} failed executing job {}: {}", workerId, job.getId(), e.getMessage(), e);
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            String fullError = sw.toString();
            jobQueueManager.failJob(job.getId(), fullError.length() > 4000 ? fullError.substring(0, 4000) : fullError);
        }
    }

    private void executeJob(BackgroundJobEntity job) {
        String type = job.getType();
        JsonNode payload = job.getPayload();

        switch (type) {
            case "award_badges" -> {
                if (payload != null && payload.hasNonNull("userId")) {
                    UUID userId = UUID.fromString(payload.get("userId").asText());
                    statsService.awardBadges(userId);
                } else {
                    throw new IllegalArgumentException("award_badges requires userId");
                }
            }
            case "update_user_stats" -> {
                if (payload != null && payload.hasNonNull("userId") && payload.hasNonNull("mediaType")) {
                    UUID userId = UUID.fromString(payload.get("userId").asText());
                    MediaType mediaType = MediaType.valueOf(payload.get("mediaType").asText().toUpperCase());
                    statsService.updateUserStatsCache(userId, mediaType);
                } else {
                    throw new IllegalArgumentException("update_user_stats requires userId and mediaType");
                }
            }
            case "recalculate_ranks" -> pageRankAggregationService.processAllMediaTypes();
            case "sweep_ranks" -> pageRankAggregationService.sweepRanks();
            default -> log.warn("Unknown background job type: {}", type);
        }
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkStuckJobs() {
        int stuck = jobQueueManager.markStuckJobsFailed(5);
        if (stuck > 0) log.warn("Marked {} stuck jobs as failed", stuck);
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupOldJobs() {
        int deleted = jobQueueManager.cleanupCompletedJobs(7);
        if (deleted > 0) log.info("Cleaned up {} old completed background jobs", deleted);
    }
}
