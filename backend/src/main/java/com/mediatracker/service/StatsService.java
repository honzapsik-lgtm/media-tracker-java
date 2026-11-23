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
    private final FriendshipRepository friendshipRepository;
    private final UserListRepository userListRepository;

    public StatsService(UserRatingRepository userRatingRepository,
                        MediaStatsRepository mediaStatsRepository,
                        UserWatchlistRepository userWatchlistRepository,
                        UserStatsCacheRepository userStatsCacheRepository,
                        UserBadgeRepository userBadgeRepository,
                        ObjectMapper objectMapper,
                        FriendshipRepository friendshipRepository,
                        UserListRepository userListRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userListRepository = userListRepository;
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

    /** Progress towards one badge: unlocked when {@code current >= target}. */
    public record BadgeProgress(int current, int target) {
        public boolean unlocked() { return current >= target; }
    }

    private static boolean isMovie(String id) { return id.startsWith("tmdb-movie-"); }
    private static boolean isShow(String id) { return id.matches("^tmdb-tv-[0-9]+$"); }
    private static boolean isSeason(String id) { return id.matches("^tmdb-tv-[0-9]+-s[0-9]+$"); }
    private static boolean isGame(String id) { return id.startsWith("igdb-") || id.startsWith("rawg-"); }
    private static boolean isManga(String id) {
        return id.startsWith("manga-") || id.startsWith("mangadex-") || id.startsWith("anilist-manga-");
    }

    /**
     * Progress for every badge. Keep the rules in sync with BADGE_DICTIONARY in the frontend
     * (components/badges.ts) and with scripts/seed/generate_seed.py.
     */
    @Transactional(readOnly = true)
    public Map<String, BadgeProgress> badgeProgress(UUID userId) {
        List<UserRatingEntity> ratings = userRatingRepository.findByUserId(userId).stream()
                .filter(r -> r.getMediaId() != null && r.getScore() != null).toList();
        int total = ratings.size();
        int movies = (int) ratings.stream().filter(r -> isMovie(r.getMediaId())).count();
        int shows = (int) ratings.stream().filter(r -> isShow(r.getMediaId())).count();
        int seasons = (int) ratings.stream().filter(r -> isSeason(r.getMediaId())).count();
        int games = (int) ratings.stream().filter(r -> isGame(r.getMediaId())).count();
        int manga = (int) ratings.stream().filter(r -> isManga(r.getMediaId())).count();
        int reviews = (int) ratings.stream().filter(r -> r.getReviewText() != null && !r.getReviewText().isBlank()).count();
        int deep = (int) ratings.stream().filter(r -> Boolean.TRUE.equals(r.getIsDeepReview())).count();
        boolean voidStare = ratings.stream().anyMatch(r -> r.getScore() <= 20);
        boolean masterpiece = ratings.stream().anyMatch(r -> r.getScore() == 100);
        double avg = ratings.stream().mapToInt(UserRatingEntity::getScore).average().orElse(0);
        int allRounder = Math.min(Math.min(movies, shows), Math.min(games, manga));
        int friends = friendshipRepository.findAcceptedFriendships(userId).size();
        int lists = (int) userListRepository.countByUserId(userId);
        int completed = userWatchlistRepository.findByUserIdAndStatus(userId, WatchlistStatus.COMPLETED).size();

        Map<String, BadgeProgress> p = new LinkedHashMap<>();
        p.put("ratings_10", new BadgeProgress(total, 10));
        p.put("ratings_50", new BadgeProgress(total, 50));
        p.put("ratings_100", new BadgeProgress(total, 100));
        p.put("ratings_250", new BadgeProgress(total, 250));
        p.put("ratings_500", new BadgeProgress(total, 500));
        p.put("movies_25", new BadgeProgress(movies, 25));
        p.put("shows_25", new BadgeProgress(shows, 25));
        p.put("seasons_10", new BadgeProgress(seasons, 10));
        p.put("games_10", new BadgeProgress(games, 10));
        p.put("manga_10", new BadgeProgress(manga, 10));
        p.put("all_rounder", new BadgeProgress(allRounder, 5));
        p.put("reviews_10", new BadgeProgress(reviews, 10));
        p.put("reviews_50", new BadgeProgress(reviews, 50));
        p.put("deep_10", new BadgeProgress(deep, 10));
        p.put("void_stare", new BadgeProgress(voidStare ? 1 : 0, 1));
        p.put("masterpiece", new BadgeProgress(masterpiece ? 1 : 0, 1));
        // Average-based badges need 25 ratings first; progress shows how many are still missing.
        p.put("tough_crowd", new BadgeProgress(total >= 25 && avg <= 55 ? 25 : Math.min(total, 24), 25));
        p.put("golden_heart", new BadgeProgress(total >= 25 && avg >= 85 ? 25 : Math.min(total, 24), 25));
        p.put("social_10", new BadgeProgress(friends, 10));
        p.put("curator", new BadgeProgress(lists, 1));
        p.put("finisher", new BadgeProgress(completed, 25));
        return p;
    }

    /**
     * Profile statistics computed from all of a user's ratings and lists (the stats cache only has
     * per-type totals): per-type counts and averages, reviews, monthly activity, release decades,
     * time spent, rating streaks and the top-rated titles.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> profileInsights(UUID userId) {
        List<UserRatingEntity> ratings = userRatingRepository.findByUserId(userId).stream()
                .filter(r -> r.getMediaId() != null && r.getScore() != null).toList();

        Map<String, List<Integer>> scoresByType = new LinkedHashMap<>();
        for (String t : List.of("movie", "show", "season", "game", "manga")) scoresByType.put(t, new ArrayList<>());
        for (UserRatingEntity r : ratings) {
            String id = r.getMediaId();
            String t = isMovie(id) ? "movie" : isShow(id) ? "show" : isSeason(id) ? "season" : isGame(id) ? "game" : isManga(id) ? "manga" : null;
            if (t != null) scoresByType.get(t).add(r.getScore());
        }
        List<Map<String, Object>> byType = new ArrayList<>();
        scoresByType.forEach((t, scores) -> byType.add(Map.of(
                "type", t,
                "count", scores.size(),
                "average", scores.isEmpty() ? 0 : Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0)));

        // Ratings per month, last 12 months (oldest first)
        java.time.YearMonth thisMonth = java.time.YearMonth.now(java.time.ZoneOffset.UTC);
        Map<java.time.YearMonth, Integer> perMonth = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) perMonth.put(thisMonth.minusMonths(i), 0);
        // Distinct rating days, for streaks
        java.util.TreeSet<java.time.LocalDate> days = new java.util.TreeSet<>();
        for (UserRatingEntity r : ratings) {
            if (r.getCreatedAt() == null) continue;
            var date = r.getCreatedAt().atZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDate();
            days.add(date);
            perMonth.computeIfPresent(java.time.YearMonth.from(date), (k, v) -> v + 1);
        }
        List<Map<String, Object>> monthly = new ArrayList<>();
        perMonth.forEach((m, c) -> monthly.add(Map.of("month", m.toString(), "count", c)));

        int longest = 0, run = 0;
        java.time.LocalDate prev = null;
        for (java.time.LocalDate d : days) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            longest = Math.max(longest, run);
            prev = d;
        }
        int current = 0;
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        java.time.LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        while (days.contains(cursor)) { current++; cursor = cursor.minusDays(1); }

        // Release decades of rated titles
        Map<String, Integer> decades = new TreeMap<>();
        for (UserRatingEntity r : ratings) {
            String d = r.getMediaReleaseDate();
            if (d == null || d.length() < 4 || !d.substring(0, 4).chars().allMatch(Character::isDigit)) continue;
            int year = Integer.parseInt(d.substring(0, 4));
            if (year < 1900) continue;
            decades.merge((year / 10 * 10) + "s", 1, Integer::sum);
        }
        List<Map<String, Object>> decadeList = new ArrayList<>();
        decades.forEach((k, v) -> decadeList.add(Map.of("decade", k, "count", v)));

        // Time spent from list progress
        List<UserWatchlistEntity> watchlist = userWatchlistRepository.findByUserId(userId);
        int episodes = 0, chapters = 0, movieWatches = 0, completed = 0;
        double hours = 0;
        for (UserWatchlistEntity w : watchlist) {
            episodes += w.getEpisodesWatched() != null ? w.getEpisodesWatched() : 0;
            chapters += w.getChaptersRead() != null ? w.getChaptersRead() : 0;
            hours += w.getHoursPlayed() != null ? w.getHoursPlayed() : 0;
            movieWatches += w.getWatchCount() != null ? w.getWatchCount() : 0;
            if (w.getStatus() == WatchlistStatus.COMPLETED) completed++;
        }

        List<Map<String, Object>> topRated = ratings.stream()
                .sorted(Comparator.comparingInt(UserRatingEntity::getScore).reversed()
                        .thenComparing(UserRatingEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("mediaId", r.getMediaId());
                    m.put("title", r.getMediaTitle());
                    m.put("image", r.getMediaImage());
                    m.put("score", r.getScore());
                    return m;
                }).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalRatings", ratings.size());
        out.put("average", ratings.isEmpty() ? 0 : Math.round(ratings.stream().mapToInt(UserRatingEntity::getScore).average().orElse(0) * 10) / 10.0);
        out.put("reviews", ratings.stream().filter(r -> r.getReviewText() != null && !r.getReviewText().isBlank()).count());
        out.put("deepReviews", ratings.stream().filter(r -> Boolean.TRUE.equals(r.getIsDeepReview())).count());
        out.put("perfectScores", ratings.stream().filter(r -> r.getScore() == 100).count());
        out.put("byType", byType);
        out.put("monthly", monthly);
        out.put("decades", decadeList);
        out.put("currentStreak", current);
        out.put("longestStreak", longest);
        out.put("episodesWatched", episodes);
        out.put("chaptersRead", chapters);
        out.put("hoursPlayed", Math.round(hours));
        out.put("movieWatches", movieWatches);
        out.put("completed", completed);
        out.put("topRated", topRated);
        return out;
    }

    @Transactional
    public void awardBadges(UUID userId) {
        badgeProgress(userId).forEach((bId, progress) -> {
            if (!progress.unlocked()) return;
            UserBadgeId id = new UserBadgeId(userId, bId);
            if (!userBadgeRepository.existsById(id)) {
                UserBadgeEntity b = new UserBadgeEntity();
                b.setUserId(userId);
                b.setBadgeId(bId);
                userBadgeRepository.save(b);
            }
        });
    }
}
