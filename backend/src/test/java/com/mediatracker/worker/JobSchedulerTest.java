package com.mediatracker.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.entity.BackgroundJobEntity;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.service.PageRankAggregationService;
import com.mediatracker.service.StatsService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobSchedulerTest {
    private final JobQueueManager queue = mock(JobQueueManager.class);
    private final StatsService stats = mock(StatsService.class);
    private final PageRankAggregationService ranks = mock(PageRankAggregationService.class);
    private final JobScheduler scheduler = new JobScheduler(queue, stats, ranks);

    private BackgroundJobEntity job(String id, String type) {
        BackgroundJobEntity job = new BackgroundJobEntity();
        job.setId(id);
        job.setType(type);
        return job;
    }

    @Test
    void emptyQueueStopsImmediately() {
        when(queue.claimJob(anyString())).thenReturn(Optional.empty());
        var result = scheduler.processBatch(10);
        assertTrue(result.ok());
        assertEquals(0, result.processed());
        verify(queue).claimJob(result.workerId());
        verifyNoMoreInteractions(queue);
        verifyNoInteractions(stats, ranks);
    }

    @Test
    void batchIsBoundedAndExecutesBothRankHandlers() {
        when(queue.claimJob(anyString())).thenReturn(Optional.of(job("1", "recalculate_ranks")),
                Optional.of(job("2", "sweep_ranks")));
        var result = scheduler.processBatch(2);
        assertEquals(2, result.processed());
        assertEquals(2, result.completed());
        assertEquals(0, result.retried());
        assertEquals(0, result.failed());
        verify(queue, times(2)).claimJob(result.workerId());
        verify(ranks).processAllMediaTypes();
        verify(ranks).sweepRanks();
        verify(queue).completeJob("1");
        verify(queue).completeJob("2");
    }

    @Test
    void dispatchesUserPayloads() {
        UUID userId = UUID.randomUUID();
        var badges = job("badges", "award_badges");
        var update = job("stats", "update_user_stats");
        var payload = new ObjectMapper().createObjectNode().put("userId", userId.toString()).put("mediaType", "movie");
        badges.setPayload(payload);
        update.setPayload(payload);
        when(queue.claimJob(anyString())).thenReturn(Optional.of(badges), Optional.of(update), Optional.empty());
        assertEquals(2, scheduler.processBatch(10).completed());
        verify(stats).awardBadges(userId);
        verify(stats).updateUserStatsCache(userId, MediaType.MOVIE);
    }

    @Test
    void failuresAreCountedAndDoNotStopRemainingJobs() {
        when(queue.claimJob(anyString())).thenReturn(Optional.of(job("retry", "award_badges")),
                Optional.of(job("unknown", "legacy_missing_handler")),
                Optional.of(job("success", "sweep_ranks")), Optional.empty());
        when(queue.failJob(eq("retry"), contains("requires userId"))).thenReturn(true);
        when(queue.failJob(eq("unknown"), contains("Unknown background job type"))).thenReturn(false);
        var result = scheduler.processBatch(10);
        assertEquals(3, result.processed());
        assertEquals(1, result.completed());
        assertEquals(1, result.retried());
        assertEquals(1, result.failed());
        verify(queue, never()).completeJob("retry");
        verify(queue, never()).completeJob("unknown");
        verify(queue).completeJob("success");
    }

    @Test
    void handlerExceptionIsRecordedAndTruncated() {
        when(queue.claimJob(anyString())).thenReturn(Optional.of(job("1", "sweep_ranks")));
        doThrow(new IllegalStateException("x".repeat(5000))).when(ranks).sweepRanks();
        when(queue.failJob(eq("1"), anyString())).thenReturn(true);
        assertEquals(1, scheduler.processBatch(1).retried());
        verify(queue).failJob(eq("1"), argThat(error -> error.length() == 4000));
        verify(queue, never()).completeJob(anyString());
    }

    @Test
    void invalidBatchSizesDoNotClaimJobs() {
        for (int size : new int[] { -1, 0, 101 }) {
            assertThrows(IllegalArgumentException.class, () -> scheduler.processBatch(size));
        }
        verifyNoInteractions(queue);
    }

    @Test
    void infrastructureFailuresAreNotReportedAsSuccess() {
        when(queue.claimJob(anyString())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> scheduler.processBatch(10));
    }

    @Test
    void scheduledProcessingUsesTheSameBatchPath() {
        when(queue.claimJob(anyString())).thenReturn(Optional.of(job("1", "sweep_ranks")));
        scheduler.processNextJob();
        verify(queue).claimJob(anyString());
        verify(ranks).sweepRanks();
        verify(queue).completeJob("1");
    }
}
