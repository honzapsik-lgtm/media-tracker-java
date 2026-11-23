package com.mediatracker.service;

import com.mediatracker.client.TmdbClient;
import com.mediatracker.model.dto.DiscoverItemDto;
import com.mediatracker.model.dto.TwinTasteRecommendationDto;
import com.mediatracker.model.dto.TwinTasteRecommendationDto.*;
import com.mediatracker.model.entity.GlobalRankingEntity;
import com.mediatracker.model.entity.MediaStatsEntity;
import com.mediatracker.model.entity.UserEntity;
import com.mediatracker.model.entity.UserRatingEntity;
import com.mediatracker.model.entity.UserWatchlistEntity;
import com.mediatracker.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final UserRatingRepository userRatingRepository;
    private final UserWatchlistRepository userWatchlistRepository;
    private final UserRepository userRepository;
    private final MediaStatsRepository mediaStatsRepository;
    private final GlobalRankingRepository globalRankingRepository;
    private final TmdbClient tmdbClient;

    public RecommendationService(UserRatingRepository userRatingRepository,
                                 UserWatchlistRepository userWatchlistRepository,
                                 UserRepository userRepository,
                                 MediaStatsRepository mediaStatsRepository,
                                 GlobalRankingRepository globalRankingRepository,
                                 TmdbClient tmdbClient) {
        this.userRatingRepository = userRatingRepository;
        this.userWatchlistRepository = userWatchlistRepository;
        this.userRepository = userRepository;
        this.mediaStatsRepository = mediaStatsRepository;
        this.globalRankingRepository = globalRankingRepository;
        this.tmdbClient = tmdbClient;
    }

    @Transactional(readOnly = true)
    public TwinTasteRecommendationDto getRecommendations(UUID currentUserId) {
        TwinTasteRecommendationDto dto = new TwinTasteRecommendationDto();

        if (currentUserId == null) {
            dto.setAuthenticated(false);
            dto.setHasRatings(false);
            dto.setUserRatingsCount(0);
            dto.setTopTwins(List.of());
            dto.setTwinRecommendations(List.of());
            dto.setContentRecommendation(null);
            return dto;
        }

        dto.setAuthenticated(true);

        List<UserRatingEntity> myRatings = userRatingRepository.findByUserId(currentUserId);
        dto.setUserRatingsCount(myRatings.size());

        if (myRatings.isEmpty()) {
            dto.setHasRatings(false);
            dto.setTopTwins(List.of());
            dto.setTwinRecommendations(List.of());
            dto.setContentRecommendation(null);
            return dto;
        }

        dto.setHasRatings(true);

        // 1. Collect media IDs to exclude (already rated or in watchlist)
        Set<String> excludedMediaIds = new HashSet<>();
        myRatings.forEach(r -> {
            if (r.getMediaId() == null) return;
            excludedMediaIds.add(r.getMediaId());
            // Rating a season means you've seen the show, so don't recommend the show itself.
            var season = SEASON_ID.matcher(r.getMediaId());
            if (season.matches()) excludedMediaIds.add(season.group(1));
        });

        List<UserWatchlistEntity> myWatchlist = userWatchlistRepository.findByUserId(currentUserId);
        myWatchlist.forEach(w -> {
            if (w.getMediaId() != null) excludedMediaIds.add(w.getMediaId());
        });

        Map<String, Integer> myScoreMap = myRatings.stream()
                .filter(r -> r.getMediaId() != null && r.getScore() != null)
                .collect(Collectors.toMap(UserRatingEntity::getMediaId, UserRatingEntity::getScore, (a, b) -> a));

        // 2. Collaborative Filtering: Taste Twins
        List<TwinProfile> topTwins = new ArrayList<>();
        List<TwinMediaRecommendation> twinRecommendations = new ArrayList<>();

        if (!myScoreMap.isEmpty()) {
            // Compare on "comparable" scores: direct ratings, plus a whole-show score averaged from season
            // ratings when someone rated only seasons. Shows I rated (directly or via seasons) also pull in
            // other users' season ratings of those shows.
            Map<String, ComparableScore> myComparable = comparableScores(myRatings);
            List<String> myMediaIds = new ArrayList<>(myComparable.keySet());
            List<String> myShowIds = myComparable.keySet().stream().filter(k -> SHOW_ID.matcher(k).matches()).toList();
            List<UserRatingEntity> overlapping = userRatingRepository.findOverlappingWithSeasons(
                    myMediaIds, myShowIds.isEmpty() ? List.of("-") : myShowIds, currentUserId);

            Map<UUID, List<UserRatingEntity>> byOtherUser = overlapping.stream()
                    .collect(Collectors.groupingBy(UserRatingEntity::getUserId));

            record CandidateTwin(UUID userId, int matchPct, int sharedCount) {}
            List<CandidateTwin> candidates = new ArrayList<>();

            for (Map.Entry<UUID, List<UserRatingEntity>> entry : byOtherUser.entrySet()) {
                UUID otherUid = entry.getKey();
                Map<String, ComparableScore> theirs = comparableScores(entry.getValue());

                double totalProximity = 0;
                int sharedCount = 0;
                for (Map.Entry<String, ComparableScore> mine : myComparable.entrySet()) {
                    ComparableScore other = theirs.get(mine.getKey());
                    // When both sides only have season-derived show scores, the seasons themselves are
                    // compared directly, so skip the derived show score to avoid counting it twice.
                    if (other == null || (mine.getValue().derived() && other.derived())) continue;
                    double diff = Math.abs(mine.getValue().score() - other.score());
                    totalProximity += (1.0 - (diff / 100.0));
                    sharedCount++;
                }
                if (sharedCount == 0) continue;

                double avgMatch = totalProximity / sharedCount;
                double confidenceFactor = Math.min(1.0, 0.70 + (sharedCount * 0.10));
                int matchPct = Math.min(99, Math.max(30, (int) Math.round(avgMatch * confidenceFactor * 100)));

                if (matchPct >= 50) {
                    candidates.add(new CandidateTwin(otherUid, matchPct, sharedCount));
                }
            }

            candidates.sort((a, b) -> {
                if (b.matchPct() != a.matchPct()) return Integer.compare(b.matchPct(), a.matchPct());
                return Integer.compare(b.sharedCount(), a.sharedCount());
            });

            List<CandidateTwin> bestCandidates = candidates.stream().limit(4).toList();
            List<UUID> twinUids = bestCandidates.stream().map(CandidateTwin::userId).toList();
            Map<UUID, UserEntity> userMap = userRepository.findAllById(twinUids).stream()
                    .collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));

            for (CandidateTwin cand : bestCandidates) {
                UserEntity u = userMap.get(cand.userId());
                if (u != null) {
                    topTwins.add(new TwinProfile(
                            u.getId(),
                            u.getUsername(),
                            u.getName(),
                            u.getImage(),
                            cand.matchPct(),
                            cand.sharedCount()
                    ));
                }
            }

            // Gather candidate recommendations from twins (items they gave >= 75)
            Map<String, TwinMediaRecommendation> recMap = new LinkedHashMap<>();
            for (TwinProfile twin : topTwins) {
                List<UserRatingEntity> twinRatings = userRatingRepository.findByUserIdAndScoreGreaterThanEqual(twin.id(), 75);
                for (UserRatingEntity r : twinRatings) {
                    if (r.getMediaId() == null || excludedMediaIds.contains(r.getMediaId())) continue;

                    if (!recMap.containsKey(r.getMediaId())) {
                        String type = inferTypeFromMediaId(r.getMediaId());
                        recMap.put(r.getMediaId(), new TwinMediaRecommendation(
                                r.getMediaId(),
                                r.getMediaTitle() != null ? r.getMediaTitle() : r.getMediaId(),
                                r.getMediaImage(),
                                type,
                                r.getMediaReleaseDate(),
                                null,
                                null,
                                r.getScore(),
                                twin
                        ));
                    }
                }
            }

            // Enrich twin recommendations with DB community score and list rank
            List<String> recMediaIds = new ArrayList<>(recMap.keySet());
            if (!recMediaIds.isEmpty()) {
                Map<String, Integer> statsMap = mediaStatsRepository.findAllById(recMediaIds).stream()
                        .filter(s -> s.getCommunityAverage() != null && s.getTotalRatings() != null && s.getTotalRatings() > 0)
                        .collect(Collectors.toMap(MediaStatsEntity::getId, s -> s.getCommunityAverage().intValue(), (a, b) -> a));

                Map<String, Integer> rankMap = globalRankingRepository.findAllById(recMediaIds).stream()
                        .filter(r -> r.getRank() != null)
                        .collect(Collectors.toMap(GlobalRankingEntity::getMediaId, GlobalRankingEntity::getRank, (a, b) -> a));

                for (TwinMediaRecommendation rec : recMap.values()) {
                    Integer commScore = statsMap.get(rec.id());
                    Integer listRank = rankMap.get(rec.id());
                    twinRecommendations.add(new TwinMediaRecommendation(
                            rec.id(),
                            rec.title(),
                            rec.image(),
                            rec.type(),
                            rec.releaseDate(),
                            commScore,
                            listRank,
                            rec.twinScore(),
                            rec.twin()
                    ));
                }
            }
        }

        dto.setTopTwins(topTwins);
        dto.setTwinRecommendations(twinRecommendations);

        // 3. Content-based: "Because You Loved [Title]" for up to 3 of the user's top-rated TMDb titles
        Set<String> shown = new HashSet<>(excludedMediaIds);
        List<UserRatingEntity> topTmdb = myRatings.stream()
                .filter(r -> r.getScore() != null && r.getScore() >= 75 && r.getMediaId() != null && r.getMediaId().startsWith("tmdb-"))
                .sorted(Comparator.comparingInt(UserRatingEntity::getScore).reversed())
                .toList();

        List<ContentRecommendation> contentRecs = new ArrayList<>();
        for (UserRatingEntity source : topTmdb) {
            if (contentRecs.size() >= 3) break;
            TmdbRef ref = TmdbRef.parse(source.getMediaId());
            if (ref == null) continue;
            List<DiscoverItemDto> items = takeUnseen(tmdbClient.getRecommendations(ref.id(), ref.type()), shown, 15);
            if (items.isEmpty()) continue;
            attachStats(items);
            String sourceTitle = source.getMediaTitle() != null ? source.getMediaTitle() : "your favorite title";
            contentRecs.add(new ContentRecommendation(source.getMediaId(), sourceTitle, source.getScore(), items));
        }
        dto.setContentRecommendations(contentRecs);
        dto.setContentRecommendation(contentRecs.isEmpty() ? null : contentRecs.get(0));

        // 4. Genre-based: the genres that appear most in the user's well-rated TMDb titles
        dto.setGenreRecommendations(buildGenreRecommendations(myRatings, shown));
        return dto;
    }

    private static final java.util.regex.Pattern SHOW_ID = java.util.regex.Pattern.compile("^tmdb-tv-[0-9]+$");
    private static final java.util.regex.Pattern SEASON_ID = java.util.regex.Pattern.compile("^(tmdb-tv-[0-9]+)-s[0-9]+$");

    /** A score used for twin matching; {@code derived} when it is a show score averaged from season ratings. */
    private record ComparableScore(double score, boolean derived) {}

    /**
     * Direct ratings by media id, plus, for every show rated only by season, the average of those
     * season ratings under the show's id (tmdb-tv-<id>). A direct show rating always wins.
     */
    static Map<String, ComparableScore> comparableScores(List<UserRatingEntity> ratings) {
        Map<String, ComparableScore> result = new HashMap<>();
        Map<String, List<Integer>> seasonsByShow = new HashMap<>();
        for (UserRatingEntity r : ratings) {
            if (r.getMediaId() == null || r.getScore() == null) continue;
            result.putIfAbsent(r.getMediaId(), new ComparableScore(r.getScore(), false));
            var m = SEASON_ID.matcher(r.getMediaId());
            if (m.matches()) seasonsByShow.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(r.getScore());
        }
        seasonsByShow.forEach((showId, scores) -> {
            if (!result.containsKey(showId)) {
                double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
                result.put(showId, new ComparableScore(avg, true));
            }
        });
        return result;
    }

    private record TmdbRef(String type, int id) {
        static TmdbRef parse(String mediaId) {
            String[] parts = mediaId.split("-");
            if (parts.length < 3 || !"tmdb".equals(parts[0])) return null;
            try {
                return new TmdbRef(parts[1], Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private record GenreInfo(int id, String slug) {}

    /** TMDb genre name -> id and the matching Discover filter slug, per TMDb media type. */
    private static final Map<String, GenreInfo> TMDB_MOVIE_GENRES = Map.ofEntries(
            Map.entry("Action", new GenreInfo(28, "action")), Map.entry("Adventure", new GenreInfo(12, "adventure")),
            Map.entry("Animation", new GenreInfo(16, "animation")), Map.entry("Comedy", new GenreInfo(35, "comedy")),
            Map.entry("Crime", new GenreInfo(80, "crime")), Map.entry("Documentary", new GenreInfo(99, "documentary")),
            Map.entry("Drama", new GenreInfo(18, "drama")), Map.entry("Family", new GenreInfo(10751, "family")),
            Map.entry("Fantasy", new GenreInfo(14, "fantasy")), Map.entry("History", new GenreInfo(36, "history")),
            Map.entry("Horror", new GenreInfo(27, "horror")), Map.entry("Music", new GenreInfo(10402, "music")),
            Map.entry("Mystery", new GenreInfo(9648, "mystery")), Map.entry("Romance", new GenreInfo(10749, "romance")),
            Map.entry("Science Fiction", new GenreInfo(878, "scifi")), Map.entry("TV Movie", new GenreInfo(10770, "tvmovie")),
            Map.entry("Thriller", new GenreInfo(53, "thriller")), Map.entry("War", new GenreInfo(10752, "war")),
            Map.entry("Western", new GenreInfo(37, "western")));
    private static final Map<String, GenreInfo> TMDB_TV_GENRES = Map.ofEntries(
            Map.entry("Action & Adventure", new GenreInfo(10759, "actionadventure")), Map.entry("Animation", new GenreInfo(16, "animation")),
            Map.entry("Comedy", new GenreInfo(35, "comedy")), Map.entry("Crime", new GenreInfo(80, "crime")),
            Map.entry("Documentary", new GenreInfo(99, "documentary")), Map.entry("Drama", new GenreInfo(18, "drama")),
            Map.entry("Family", new GenreInfo(10751, "family")), Map.entry("Kids", new GenreInfo(10762, "kids")),
            Map.entry("Mystery", new GenreInfo(9648, "mystery")), Map.entry("Reality", new GenreInfo(10764, "reality")),
            Map.entry("Sci-Fi & Fantasy", new GenreInfo(10765, "scififantasy")), Map.entry("Soap", new GenreInfo(10766, "soap")),
            Map.entry("War & Politics", new GenreInfo(10768, "warpolitics")), Map.entry("Western", new GenreInfo(37, "western")));

    private List<GenreRecommendation> buildGenreRecommendations(List<UserRatingEntity> myRatings, Set<String> shown) {
        record GenreKey(String tmdbType, String name) {}
        Map<GenreKey, Double> weights = new HashMap<>();

        myRatings.stream()
                .filter(r -> r.getScore() != null && r.getScore() >= 60 && r.getMediaId() != null && r.getMediaId().startsWith("tmdb-"))
                .sorted(Comparator.comparingInt(UserRatingEntity::getScore).reversed())
                .limit(25) // details are cached for 7 days, so this is cheap after the first visit
                .forEach(r -> {
                    TmdbRef ref = TmdbRef.parse(r.getMediaId());
                    if (ref == null) return;
                    try {
                        tmdbClient.getDetails(ref.id(), ref.type()).ifPresent(details -> {
                            for (String g : details.getGenres()) {
                                weights.merge(new GenreKey(ref.type(), g), r.getScore() - 50.0, Double::sum);
                            }
                        });
                    } catch (Exception e) {
                        log.warn("Genre lookup failed for {}: {}", r.getMediaId(), e.getMessage());
                    }
                });

        List<GenreKey> ranked = weights.entrySet().stream()
                .sorted(Map.Entry.<GenreKey, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        List<GenreRecommendation> result = new ArrayList<>();
        for (GenreKey key : ranked) {
            if (result.size() >= 3) break;
            boolean isMovie = "movie".equals(key.tmdbType());
            GenreInfo genre = (isMovie ? TMDB_MOVIE_GENRES : TMDB_TV_GENRES).get(key.name());
            if (genre == null) continue;
            List<DiscoverItemDto> pool = new ArrayList<>(tmdbClient.discover(key.tmdbType(), List.of(genre.id()), List.of(), null, "all_time", 1));
            pool.addAll(tmdbClient.discover(key.tmdbType(), List.of(genre.id()), List.of(), null, "all_time", 2));
            List<DiscoverItemDto> items = takeUnseen(pool, shown, 15);
            if (items.isEmpty()) continue;
            attachStats(items);
            result.add(new GenreRecommendation(key.name(), genre.slug(), isMovie ? "movie" : "show", items));
        }
        return result;
    }

    /** Picks up to {@code limit} items whose ids are not in {@code shown}, and marks them as shown. */
    private static List<DiscoverItemDto> takeUnseen(List<DiscoverItemDto> items, Set<String> shown, int limit) {
        List<DiscoverItemDto> out = new ArrayList<>();
        for (DiscoverItemDto item : items) {
            if (out.size() >= limit) break;
            if (item.getId() != null && shown.add(item.getId())) out.add(item);
        }
        return out;
    }

    private void attachStats(List<DiscoverItemDto> items) {
        List<String> ids = items.stream().map(DiscoverItemDto::getId).toList();
        if (ids.isEmpty()) return;
        Map<String, Integer> statsMap = mediaStatsRepository.findAllById(ids).stream()
                .filter(s -> s.getCommunityAverage() != null && s.getTotalRatings() != null && s.getTotalRatings() > 0)
                .collect(Collectors.toMap(MediaStatsEntity::getId, s -> s.getCommunityAverage().intValue(), (a, b) -> a));
        Map<String, Integer> rankMap = globalRankingRepository.findAllById(ids).stream()
                .filter(r -> r.getRank() != null)
                .collect(Collectors.toMap(GlobalRankingEntity::getMediaId, GlobalRankingEntity::getRank, (a, b) -> a));
        for (DiscoverItemDto item : items) {
            item.setCommunityScore(statsMap.get(item.getId()));
            item.setListRank(rankMap.get(item.getId()));
        }
    }

    private String inferTypeFromMediaId(String mediaId) {
        if (mediaId == null) return "movie";
        if (mediaId.startsWith("tmdb-movie-")) return "movie";
        if (mediaId.startsWith("tmdb-tv-")) return "show";
        if (mediaId.startsWith("igdb-") || mediaId.startsWith("rawg-")) return "game";
        if (mediaId.startsWith("anilist-") || mediaId.startsWith("mangadex-") || mediaId.startsWith("manga-")) return "manga";
        return "movie";
    }
}
