package com.mediatracker.service;

import com.mediatracker.model.entity.UserWatchlistEntity;
import com.mediatracker.model.enums.ActivityType;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.model.enums.WatchlistStatus;
import com.mediatracker.repository.UserWatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private final UserWatchlistRepository userWatchlistRepository;
    private final StatsService statsService;
    private final ActivityService activityService;

    public WatchlistService(UserWatchlistRepository userWatchlistRepository,
                            StatsService statsService,
                            ActivityService activityService) {
        this.userWatchlistRepository = userWatchlistRepository;
        this.statsService = statsService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public Page<UserWatchlistEntity> getUserWatchlist(UUID userId, int page, int limit, MediaType mediaType, WatchlistStatus status) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("addedAt").descending());
        if (mediaType != null && status != null) {
            return userWatchlistRepository.findByUserIdAndMediaTypeAndStatus(userId, mediaType, status, pageRequest);
        } else if (mediaType != null) {
            return userWatchlistRepository.findByUserIdAndMediaType(userId, mediaType, pageRequest);
        } else if (status != null) {
            return userWatchlistRepository.findByUserIdAndStatus(userId, status, pageRequest);
        } else {
            return userWatchlistRepository.findByUserId(userId, pageRequest);
        }
    }

    @Transactional(readOnly = true)
    public Optional<UserWatchlistEntity> getWatchlistItem(UUID userId, String mediaId) {
        return userWatchlistRepository.findByUserIdAndMediaId(userId, mediaId);
    }

    @Transactional
    public UserWatchlistEntity upsertWatchlist(UUID userId,
                                               String mediaId,
                                               String title,
                                               String image,
                                               MediaType mediaType,
                                               WatchlistStatus status,
                                               Integer episodesWatched,
                                               Integer chaptersRead,
                                               Integer volumesRead,
                                               Double hoursPlayed,
                                               String platform,
                                               Integer watchCount) {
        if (mediaId == null || mediaId.isBlank()) {
            throw new IllegalArgumentException("mediaId is required");
        }

        MediaType resolvedType = mediaType != null ? mediaType : statsService.inferMediaType(mediaId);

        UserWatchlistEntity item = userWatchlistRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseGet(() -> {
                    UserWatchlistEntity newItem = new UserWatchlistEntity();
                    newItem.setUserId(userId);
                    newItem.setMediaId(mediaId);
                    newItem.setMediaType(resolvedType);
                    newItem.setAddedAt(OffsetDateTime.now());
                    return newItem;
                });

        WatchlistStatus oldStatus = item.getStatus();

        if (title != null) item.setMediaTitle(title);
        if (image != null) item.setMediaImage(image);
        if (resolvedType != null) item.setMediaType(resolvedType);

        if (status != null) {
            item.setStatus(status);
        }

        if (episodesWatched != null) item.setEpisodesWatched(episodesWatched);
        if (chaptersRead != null) item.setChaptersRead(chaptersRead);
        if (volumesRead != null) item.setVolumesRead(volumesRead);
        if (hoursPlayed != null) item.setHoursPlayed(hoursPlayed);
        if (platform != null) item.setPlatform(platform);
        if (watchCount != null) item.setWatchCount(watchCount);

        if (item.getStatus() == WatchlistStatus.IN_PROGRESS && item.getStartedAt() == null) {
            item.setStartedAt(OffsetDateTime.now());
        }
        if (item.getStatus() == WatchlistStatus.COMPLETED && item.getFinishedAt() == null) {
            item.setFinishedAt(OffsetDateTime.now());
        }

        UserWatchlistEntity saved = userWatchlistRepository.save(item);
        statsService.updateUserStatsCache(userId, resolvedType);

        // Activity log
        if (status != null && status != oldStatus) {
            Map<String, Object> data = Map.of("status", status.name());
            activityService.logActivity(userId, ActivityType.WATCHLIST_STATUS, mediaId, item.getMediaTitle(), item.getMediaImage(), resolvedType, data);
        } else if (episodesWatched != null && episodesWatched > 0) {
            Map<String, Object> data = Map.of("episodesWatched", episodesWatched);
            activityService.logActivity(userId, ActivityType.EPISODES_WATCHED, mediaId, item.getMediaTitle(), item.getMediaImage(), resolvedType, data);
        } else if (chaptersRead != null && chaptersRead > 0) {
            Map<String, Object> data = Map.of("chaptersRead", chaptersRead);
            activityService.logActivity(userId, ActivityType.CHAPTERS_READ, mediaId, item.getMediaTitle(), item.getMediaImage(), resolvedType, data);
        }

        return saved;
    }

    @Transactional
    public boolean deleteWatchlist(UUID userId, String mediaId) {
        Optional<UserWatchlistEntity> existing = userWatchlistRepository.findByUserIdAndMediaId(userId, mediaId);
        if (existing.isPresent()) {
            MediaType type = existing.get().getMediaType();
            userWatchlistRepository.delete(existing.get());
            if (type != null) {
                statsService.updateUserStatsCache(userId, type);
            }
            return true;
        }
        return false;
    }
}
