package com.mediatracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.entity.*;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.model.enums.WatchlistStatus;
import com.mediatracker.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    private final UserRatingRepository userRatingRepository;
    private final MediaStatsRepository mediaStatsRepository;
    private final UserWatchlistRepository userWatchlistRepository;
    private final UserStatsCacheRepository userStatsCacheRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ObjectMapper objectMapper;

    public StatsService(UserRatingRepository userRatingRepository,
                        MediaStatsRepository mediaStatsRepository,
                        UserWatchlistRepository userWatchlistRepository,
                        UserStatsCacheRepository userStatsCacheRepository,
                        UserBadgeRepository userBadgeRepository,
                        ObjectMapper objectMapper) {
        this.userRatingRepository = userRatingRepository;
        this.mediaStatsRepository = mediaStatsRepository;
        this.userWatchlistRepository = userWatchlistRepository;
        this.userStatsCacheRepository = userStatsCacheRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.objectMapper = objectMapper;
    }

    public MediaType inferMediaType(String mediaId) {
        if (mediaId == null) return MediaType.OTHER;
        String[] parts = mediaId.split("-");
        if (parts[0].equalsIgnoreCase("tmdb") && parts.length > 1 && parts[1].equalsIgnoreCase("movie")) {
            return MediaType.MOVIE;
        }
        if (parts[0].equalsIgnoreCase("tmdb") && parts.length > 1 && (parts[1].equalsIgnoreCase("tv") || parts[1].equalsIgnoreCase("show"))) {
            if (mediaId.contains("-e") || parts.length == 5) return MediaType.EPISODE;
            if (mediaId.contains("-s") || parts.length == 4) return MediaType.SEASON;
            return MediaType.SHOW;
        }
        if (parts.length > 1 && parts[1].equalsIgnoreCase("game") || parts[0].equalsIgnoreCase("rawg") || parts[0].equalsIgnoreCase("igdb")) {
            return MediaType.GAME;
        }
        if (parts.length > 1 && parts[1].equalsIgnoreCase("manga") || parts[0].equalsIgnoreCase("manga") || parts[0].equalsIgnoreCase("mangadex") || parts[0].equalsIgnoreCase("anilist")) {
            return MediaType.MANGA;
        }
        return MediaType.OTHER;
    }

    @Transactional
    public void refreshMediaStats(String mediaId, MediaType mediaType) {
        if (mediaType == null) mediaType = inferMediaType(mediaId);

        List<UserRatingEntity> ratings = userRatingRepository.findByMediaId(mediaId);
        long count = ratings.size();

        if (count == 0) {
            mediaStatsRepository.deleteById(mediaId);
            return;
        }

        double sum = 0;
        for (UserRatingEntity r : ratings) {
            sum += r.getScore();
        }
        double avg = Math.round(sum / count);

        MediaStatsEntity stats = mediaStatsRepository.findById(mediaId).orElseGet(() -> {
            MediaStatsEntity s = new MediaStatsEntity();
            s.setId(mediaId);
            return s;
        });

        stats.setMediaType(mediaType);
        stats.setTotalRatings((int) count);
        stats.setCommunityAverage(BigDecimal.valueOf(avg));
        mediaStatsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public Optional<MediaStatsEntity> getMediaStats(String mediaId) {
        return mediaStatsRepository.findById(mediaId);
    }

    public Map<String, Integer> calculateCriteriaAverages(List<UserRatingEntity> deepReviews) {
        Map<String, Double> sums = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        for (UserRatingEntity r : deepReviews) {
            Object rawScores = r.getCriteriaScores();
            if (rawScores instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    if (entry.getValue() instanceof Number n) {
                        sums.put(key, sums.getOrDefault(key, 0.0) + n.doubleValue());
                        counts.put(key, counts.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }

        Map<String, Integer> result = new HashMap<>();
        for (String k : sums.keySet()) {
            result.put(k, (int) Math.round(sums.get(k) / counts.get(k)));
        }
        return result;
    }

    @Transactional
    public void updateUserStatsCache(UUID userId, MediaType mediaType) {
        List<UserRatingEntity> allRatings = userRatingRepository.findByUserId(userId);
        List<UserRatingEntity> typeRatings = allRatings.stream()
                .filter(r -> inferMediaType(r.getMediaId()) == mediaType)
                .toList();

        List<UserWatchlistEntity> watchlist = userWatchlistRepository.findByUserIdAndMediaType(userId, mediaType);

        int totalCount = typeRatings.size();
        int averageScore = 0;
        int highestScore = 0;
        int lowestScore = 0;
        Map<String, Integer> scoreDistribution = new LinkedHashMap<>();
        for (int i = 1; i <= 10; i++) {
            scoreDistribution.put(String.valueOf(i), 0);
        }

        if (totalCount > 0) {
            int sum = 0;
            highestScore = Integer.MIN_VALUE;
            lowestScore = Integer.MAX_VALUE;
            for (UserRatingEntity r : typeRatings) {
                int s = r.getScore();
                sum += s;
                if (s > highestScore) highestScore = s;
                if (s < lowestScore) lowestScore = s;

                int bucket = (int) Math.ceil(s / 10.0);
                if (bucket < 1) bucket = 1;
                if (bucket > 10) bucket = 10;
                String bKey = String.valueOf(bucket);
                scoreDistribution.put(bKey, scoreDistribution.get(bKey) + 1);
            }
            averageScore = (int) Math.round((double) sum / totalCount);
        } else {
            highestScore = 0;
            lowestScore = 0;
        }

        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("completed", 0);
        statusCounts.put("watching", 0);
        statusCounts.put("plan_to_watch", 0);
        statusCounts.put("dropped", 0);

        for (UserWatchlistEntity w : watchlist) {
            if (w.getStatus() == WatchlistStatus.COMPLETED) {
                statusCounts.put("completed", statusCounts.get("completed") + 1);
            } else if (w.getStatus() == WatchlistStatus.IN_PROGRESS) {
                statusCounts.put("watching", statusCounts.get("watching") + 1);
            } else if (w.getStatus() == WatchlistStatus.PLANNING) {
                statusCounts.put("plan_to_watch", statusCounts.get("plan_to_watch") + 1);
            } else if (w.getStatus() == WatchlistStatus.DROPPED) {
                statusCounts.put("dropped", statusCounts.get("dropped") + 1);
            }
        }

        Map<String, Object> statsJson = new HashMap<>();
        statsJson.put("total_count", totalCount);
        statsJson.put("average_score", averageScore);
        statsJson.put("highest_score", highestScore);
        statsJson.put("lowest_score", lowestScore);
        statsJson.put("score_distribution", scoreDistribution);
        statsJson.put("status_counts", statusCounts);

        UserStatsCacheId cacheId = new UserStatsCacheId(userId, mediaType);
        UserStatsCacheEntity cacheEntity = userStatsCacheRepository.findById(cacheId).orElseGet(() -> {
            UserStatsCacheEntity e = new UserStatsCacheEntity();
            e.setUserId(userId);
            e.setMediaType(mediaType);
            return e;
        });

        cacheEntity.setStatsJson(objectMapper.valueToTree(statsJson));
        userStatsCacheRepository.save(cacheEntity);
    }

    @Transactional
    public void awardBadges(UUID userId) {
        List<UserRatingEntity> ratings = userRatingRepository.findByUserId(userId);
        int totalRatingsCount = ratings.size();

        long gameRatingsCount = ratings.stream()
                .filter(r -> r.getMediaId().startsWith("rawg-") || r.getMediaId().startsWith("igdb-"))
                .count();

        long mangaRatingsCount = ratings.stream()
                .filter(r -> r.getMediaId().startsWith("manga-") || r.getMediaId().startsWith("mangadex-") || r.getMediaId().startsWith("anilist-manga-"))
                .count();

        boolean hasVoidStare = ratings.stream().anyMatch(r -> r.getScore() <= 20);
        boolean hasMasterpiece = ratings.stream().anyMatch(r -> r.getScore() == 100);

        Set<String> badgeIds = new HashSet<>();
        if (totalRatingsCount >= 10) badgeIds.add("ratings_10");
        if (totalRatingsCount >= 50) badgeIds.add("ratings_50");
        if (totalRatingsCount >= 100) badgeIds.add("ratings_100");
        if (gameRatingsCount >= 10) badgeIds.add("games_10");
        if (mangaRatingsCount >= 10) badgeIds.add("manga_10");
        if (hasVoidStare) badgeIds.add("void_stare");
        if (hasMasterpiece) badgeIds.add("masterpiece");

        for (String bId : badgeIds) {
            UserBadgeId id = new UserBadgeId(userId, bId);
            if (!userBadgeRepository.existsById(id)) {
                UserBadgeEntity b = new UserBadgeEntity();
                b.setUserId(userId);
                b.setBadgeId(bId);
                userBadgeRepository.save(b);
            }
        }
    }
}
