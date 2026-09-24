package com.mediatracker.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.entity.BackgroundJobEntity;
import com.mediatracker.repository.BackgroundJobRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobQueueManagerTest {
    private final BackgroundJobRepository repository = mock(BackgroundJobRepository.class);
    private final JobQueueManager queue = new JobQueueManager(repository, new ObjectMapper());

    private BackgroundJobEntity processingJob(int attempts) {
        var job = new BackgroundJobEntity();
        job.setId("job");
        job.setStatus("processing");
        job.setAttempts(attempts);
        job.setMaxAttempts(3);
        job.setLockedAt(LocalDateTime.now());
        job.setLockedBy("worker");
        when(repository.findById("job")).thenReturn(Optional.of(job));
        return job;
    }

    @Test
    void failureBelowAttemptLimitSchedulesRetryAndReleasesLock() {
        var job = processingJob(1);
        var before = LocalDateTime.now();
        assertTrue(queue.failJob("job", "failure"));
        assertEquals("pending", job.getStatus());
        assertFalse(job.getRunAt().isBefore(before.plusSeconds(60)));
        assertNull(job.getLockedBy());
        assertNull(job.getLockedAt());
        assertNull(job.getProcessedAt());
        assertEquals("failure", job.getLastError());
        verify(repository).save(job);
    }

    @Test
    void lastAttemptIsTerminalFailure() {
        var job = processingJob(3);
        assertFalse(queue.failJob("job", "failure"));
        assertEquals("failed", job.getStatus());
        assertNotNull(job.getProcessedAt());
        assertNull(job.getLockedBy());
        assertNull(job.getLockedAt());
        verify(repository).save(job);
    }

    @Test
    void missingClaimedJobDoesNotReturnFakeOutcome() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> queue.failJob("missing", "failure"));
    }

    @Test
    void summaryUsesRepositoryCountsAndPendingAge() {
        var job = new BackgroundJobEntity();
        job.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        when(repository.findFirstByStatusOrderByCreatedAtAsc("pending")).thenReturn(Optional.of(job));
        when(repository.countByStatus("pending")).thenReturn(7L);
        when(repository.countByStatus("processing")).thenReturn(2L);
        when(repository.countByStatus("failed")).thenReturn(3L);
        when(repository.countByStatusAndProcessedAtAfter(eq("completed"), any())).thenReturn(9L);
        when(repository.countByStatusAndLockedAtBefore(eq("processing"), any())).thenReturn(1L);
        var summary = queue.getSummary();
        assertEquals(7, summary.pending());
        assertEquals(2, summary.processing());
        assertEquals(3, summary.failed());
        assertEquals(9, summary.completedLastHour());
        assertEquals(1, summary.stuckProcessing());
        assertTrue(summary.oldestPendingAgeSeconds() >= 120);
        verify(repository).countByStatusAndLockedAtBefore(eq("processing"),
                argThat(cutoff -> Math.abs(java.time.Duration.between(cutoff, LocalDateTime.now().minusMinutes(15)).getSeconds()) < 5));
    }

    @Test
    void summaryWithoutPendingJobsHasNoAge() {
        when(repository.findFirstByStatusOrderByCreatedAtAsc("pending")).thenReturn(Optional.empty());
        assertNull(queue.getSummary().oldestPendingAgeSeconds());
    }
}
