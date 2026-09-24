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
            if (r.getMediaId() != null) excludedMediaIds.add(r.getMediaId());
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
            List<String> myMediaIds = new ArrayList<>(myScoreMap.keySet());
            List<UserRatingEntity> overlapping = userRatingRepository.findByMediaIdInAndUserIdNot(myMediaIds, currentUserId);

            Map<UUID, List<UserRatingEntity>> byOtherUser = overlapping.stream()
                    .collect(Collectors.groupingBy(UserRatingEntity::getUserId));

            record CandidateTwin(UUID userId, int matchPct, int sharedCount) {}
            List<CandidateTwin> candidates = new ArrayList<>();

            for (Map.Entry<UUID, List<UserRatingEntity>> entry : byOtherUser.entrySet()) {
                UUID otherUid = entry.getKey();
                List<UserRatingEntity> shared = entry.getValue();
                if (shared.isEmpty()) continue;

                double totalProximity = 0;
                for (UserRatingEntity r : shared) {
                    Integer myScore = myScoreMap.get(r.getMediaId());
                    if (myScore != null && r.getScore() != null) {
                        int diff = Math.abs(myScore - r.getScore());
                        totalProximity += (1.0 - (diff / 100.0));
                    }
                }

                double avgMatch = totalProximity / shared.size();
                double confidenceFactor = Math.min(1.0, 0.70 + (shared.size() * 0.10));
                int matchPct = Math.min(99, Math.max(30, (int) Math.round(avgMatch * confidenceFactor * 100)));

                if (matchPct >= 50) {
                    candidates.add(new CandidateTwin(otherUid, matchPct, shared.size()));
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

        // 3. Content-Based Recommendation: "Because You Loved [Title]"
        ContentRecommendation contentRec = null;
        UserRatingEntity topRated = myRatings.stream()
                .filter(r -> r.getScore() != null && r.getMediaId() != null)
                .max(Comparator.comparingInt(UserRatingEntity::getScore))
                .orElse(null);

        if (topRated != null && topRated.getScore() >= 75) {
            String mid = topRated.getMediaId();
            if (mid.startsWith("tmdb-")) {
                String[] parts = mid.split("-");
                if (parts.length >= 3) {
                    String tmdbType = parts[1];
                    try {
                        int tmdbId = Integer.parseInt(parts[2]);
                        List<DiscoverItemDto> rawRecs = tmdbClient.getRecommendations(tmdbId, tmdbType);

                        List<DiscoverItemDto> filtered = rawRecs.stream()
                                .filter(item -> !excludedMediaIds.contains(item.getId()))
                                .limit(15)
                                .toList();

                        // Attach DB stats & ranks to content recommendations
                        List<String> contentIds = filtered.stream().map(DiscoverItemDto::getId).toList();
                        if (!contentIds.isEmpty()) {
                            Map<String, Integer> statsMap = mediaStatsRepository.findAllById(contentIds).stream()
                                    .filter(s -> s.getCommunityAverage() != null && s.getTotalRatings() != null && s.getTotalRatings() > 0)
                                    .collect(Collectors.toMap(MediaStatsEntity::getId, s -> s.getCommunityAverage().intValue(), (a, b) -> a));

                            Map<String, Integer> rankMap = globalRankingRepository.findAllById(contentIds).stream()
                                    .filter(r -> r.getRank() != null)
                                    .collect(Collectors.toMap(GlobalRankingEntity::getMediaId, GlobalRankingEntity::getRank, (a, b) -> a));

                            for (DiscoverItemDto item : filtered) {
                                item.setCommunityScore(statsMap.get(item.getId()));
                                item.setListRank(rankMap.get(item.getId()));
                            }
                        }

                        if (!filtered.isEmpty()) {
                            String sourceTitle = topRated.getMediaTitle() != null ? topRated.getMediaTitle() : "your favorite film";
                            contentRec = new ContentRecommendation(
                                    topRated.getMediaId(),
                                    sourceTitle,
                                    topRated.getScore(),
                                    filtered
                            );
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        dto.setContentRecommendation(contentRec);
        return dto;
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
