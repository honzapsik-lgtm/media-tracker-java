package com.mediatracker.repository;

import com.mediatracker.model.entity.BackgroundJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackgroundJobRepository extends JpaRepository<BackgroundJobEntity, String> {

    @Query(value = """
        SELECT * FROM "BackgroundJob"
        WHERE status = 'pending' AND run_at <= NOW()
        ORDER BY created_at ASC
        LIMIT 1 FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    Optional<BackgroundJobEntity> claimNextJob();

    Optional<BackgroundJobEntity> findFirstByDedupeKeyAndStatusIn(String dedupeKey, List<String> statuses);

    Page<BackgroundJobEntity> findByStatus(String status, Pageable pageable);

    String ADMIN_FILTERS = """
        FROM "BackgroundJob" j
        WHERE (cast(:status as text) IS NULL OR j.status = :status)
          AND (cast(:type as text) IS NULL OR j.type = :type)
          AND (cast(:dedupeKey as text) IS NULL OR strpos(lower(coalesce(j.dedupe_key, '')), lower(:dedupeKey)) > 0)
          AND (cast(:userId as text) IS NULL OR j.payload ->> 'userId' = :userId)
          AND (cast(:q as text) IS NULL
            OR strpos(lower(j.id), lower(:q)) > 0
            OR strpos(lower(j.type), lower(:q)) > 0
            OR strpos(lower(coalesce(j.dedupe_key, '')), lower(:q)) > 0
            OR strpos(lower(coalesce(j.last_error, '')), lower(:q)) > 0
            OR strpos(lower(cast(j.payload as text)), lower(:q)) > 0)
        """;

    @Query(value = "SELECT j.* " + ADMIN_FILTERS + " ORDER BY j.created_at DESC, j.id DESC",
            countQuery = "SELECT count(*) " + ADMIN_FILTERS, nativeQuery = true)
    Page<BackgroundJobEntity> findAdminJobs(@Param("status") String status,
            @Param("type") String type, @Param("dedupeKey") String dedupeKey,
            @Param("userId") String userId, @Param("q") String q, Pageable pageable);

    long countByStatus(String status);

    long countByStatusAndProcessedAtAfter(String status, LocalDateTime cutoff);

    long countByStatusAndLockedAtBefore(String status, LocalDateTime cutoff);

    Optional<BackgroundJobEntity> findFirstByStatusOrderByCreatedAtAsc(String status);

    @Modifying
    @Query("""
        UPDATE BackgroundJobEntity j
        SET j.status = 'failed', j.lockedAt = null, j.lockedBy = null,
            j.lastError = 'Job marked failed because it was stuck in processing state',
            j.processedAt = CURRENT_TIMESTAMP
        WHERE j.status = 'processing' AND j.lockedAt < :cutoff
    """)
    int markStuckJobsFailed(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("""
        DELETE FROM BackgroundJobEntity j
        WHERE j.status IN ('completed', 'cancelled') AND j.processedAt < :cutoff
    """)
    int cleanupCompletedJobs(@Param("cutoff") LocalDateTime cutoff);
}
