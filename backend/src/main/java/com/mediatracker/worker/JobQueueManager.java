package com.mediatracker.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.entity.BackgroundJobEntity;
import com.mediatracker.repository.BackgroundJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobQueueManager {

    private static final Logger log = LoggerFactory.getLogger(JobQueueManager.class);

    private final BackgroundJobRepository backgroundJobRepository;
    private final ObjectMapper objectMapper;

    public JobQueueManager(BackgroundJobRepository backgroundJobRepository, ObjectMapper objectMapper) {
        this.backgroundJobRepository = backgroundJobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BackgroundJobEntity enqueueJob(String type,
                                         Map<String, Object> payload,
                                         String dedupeKey,
                                         LocalDateTime runAt,
                                         Integer maxAttempts) {
        if (dedupeKey != null && !dedupeKey.isBlank()) {
            Optional<BackgroundJobEntity> existing = backgroundJobRepository.findFirstByDedupeKeyAndStatusIn(
                    dedupeKey, List.of("pending", "processing")
            );
            if (existing.isPresent()) {
                log.info("Job {} deduped via key {}", existing.get().getId(), dedupeKey);
                return existing.get();
            }
        }

        BackgroundJobEntity job = new BackgroundJobEntity();
        job.setId(UUID.randomUUID().toString());
        job.setType(type);
        job.setPayload(payload != null ? objectMapper.valueToTree(payload) : objectMapper.createObjectNode());
        job.setDedupeKey(dedupeKey);
        job.setStatus("pending");
        job.setAttempts(0);
        job.setMaxAttempts(maxAttempts != null ? maxAttempts : 3);
        job.setRunAt(runAt != null ? runAt : LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        BackgroundJobEntity saved = backgroundJobRepository.save(job);
        log.info("Enqueued job {} of type {}", saved.getId(), type);
        return saved;
    }

    @Transactional
    public Optional<BackgroundJobEntity> claimJob(String workerId) {
        Optional<BackgroundJobEntity> candidateOpt = backgroundJobRepository.claimNextJob();
        if (candidateOpt.isEmpty()) return Optional.empty();

        BackgroundJobEntity job = candidateOpt.get();
        job.setStatus("processing");
        job.setLockedAt(LocalDateTime.now());
        job.setLockedBy(workerId);
        job.setAttempts(job.getAttempts() + 1);
        job.setUpdatedAt(LocalDateTime.now());

        return Optional.of(backgroundJobRepository.save(job));
    }

    @Transactional
    public void completeJob(String jobId) {
        backgroundJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus("completed");
            job.setLockedAt(null);
            job.setLockedBy(null);
            job.setProcessedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            job.setLastError(null);
            backgroundJobRepository.save(job);
            log.info("Job {} completed successfully", jobId);
        });
    }

    @Transactional
    public boolean failJob(String jobId, String errorMessage) {
        return backgroundJobRepository.findById(jobId).map(job -> {
            boolean shouldRetry = job.getAttempts() < job.getMaxAttempts();
            job.setLockedAt(null);
            job.setLockedBy(null);
            job.setLastError(errorMessage);
            job.setUpdatedAt(LocalDateTime.now());

            if (shouldRetry) {
                job.setStatus("pending");
                int delaySeconds = Math.min(60 * Math.max(job.getAttempts(), 1), 300);
                job.setRunAt(LocalDateTime.now().plusSeconds(delaySeconds));
                log.warn("Job {} failed with error, retrying in {}s: {}", jobId, delaySeconds, errorMessage);
            } else {
                job.setStatus("failed");
                job.setProcessedAt(LocalDateTime.now());
                log.error("Job {} permanently failed after {} attempts: {}", jobId, job.getAttempts(), errorMessage);
            }
            backgroundJobRepository.save(job);
            return shouldRetry;
        }).orElseThrow(() -> new IllegalStateException("Claimed job no longer exists: " + jobId));
    }

    public record JobSummary(long pending, long processing, long failed, long completedLastHour,
                             Long oldestPendingAgeSeconds, long stuckProcessing) {}

    @Transactional(readOnly = true)
    public JobSummary getSummary() {
        LocalDateTime now = LocalDateTime.now();
        Long oldestAge = backgroundJobRepository.findFirstByStatusOrderByCreatedAtAsc("pending")
                .map(job -> Math.max(0L, java.time.Duration.between(job.getCreatedAt(), now).getSeconds()))
                .orElse(null);
        return new JobSummary(backgroundJobRepository.countByStatus("pending"),
                backgroundJobRepository.countByStatus("processing"),
                backgroundJobRepository.countByStatus("failed"),
                backgroundJobRepository.countByStatusAndProcessedAtAfter("completed", now.minusHours(1)),
                oldestAge,
                backgroundJobRepository.countByStatusAndLockedAtBefore("processing", now.minusMinutes(15)));
    }

    @Transactional
    public boolean retryJob(String jobId) {
        Optional<BackgroundJobEntity> jobOpt = backgroundJobRepository.findById(jobId);
        if (jobOpt.isEmpty() || !"failed".equalsIgnoreCase(jobOpt.get().getStatus())) {
            return false;
        }
        BackgroundJobEntity job = jobOpt.get();
        job.setStatus("pending");
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setProcessedAt(null);
        job.setRunAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        backgroundJobRepository.save(job);
        return true;
    }

    @Transactional
    public boolean cancelJob(String jobId) {
        Optional<BackgroundJobEntity> jobOpt = backgroundJobRepository.findById(jobId);
        if (jobOpt.isEmpty() || !"pending".equalsIgnoreCase(jobOpt.get().getStatus())) {
            return false;
        }
        BackgroundJobEntity job = jobOpt.get();
        job.setStatus("cancelled");
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setProcessedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        backgroundJobRepository.save(job);
        return true;
    }

    @Transactional
    public int markStuckJobsFailed(int olderThanMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(olderThanMinutes);
        return backgroundJobRepository.markStuckJobsFailed(cutoff);
    }

    @Transactional
    public int cleanupCompletedJobs(int olderThanDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        return backgroundJobRepository.cleanupCompletedJobs(cutoff);
    }
}
