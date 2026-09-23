package com.mediatracker.repository;

import com.mediatracker.model.entity.UserWatchlistEntity;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.model.enums.WatchlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserWatchlistRepository extends JpaRepository<UserWatchlistEntity, UUID> {
    Optional<UserWatchlistEntity> findByUserIdAndMediaId(UUID userId, String mediaId);
    List<UserWatchlistEntity> findByUserId(UUID userId);
    List<UserWatchlistEntity> findByUserIdAndStatus(UUID userId, WatchlistStatus status);
    List<UserWatchlistEntity> findByUserIdAndMediaType(UUID userId, MediaType mediaType);
    Page<UserWatchlistEntity> findByUserId(UUID userId, Pageable pageable);
    Page<UserWatchlistEntity> findByUserIdAndMediaType(UUID userId, MediaType mediaType, Pageable pageable);
    Page<UserWatchlistEntity> findByUserIdAndStatus(UUID userId, WatchlistStatus status, Pageable pageable);
    Page<UserWatchlistEntity> findByUserIdAndMediaTypeAndStatus(UUID userId, MediaType mediaType, WatchlistStatus status, Pageable pageable);
    long countByUserId(UUID userId);
}
