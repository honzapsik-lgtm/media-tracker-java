package com.mediatracker.service;

import com.mediatracker.model.entity.*;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PageRankAggregationService {

    private static final Logger log = LoggerFactory.getLogger(PageRankAggregationService.class);

    // Ranking constants
    public static final double DAMPING_FACTOR = 0.85;
    public static final double UNIVERSAL_BASIC_INCOME_SHARE = 0.15;
    public static final int MAX_ITERATIONS = 100;
    public static final double CONVERGENCE_THRESHOLD = 0.00001;
    public static final int MIN_LIST_APPEARANCES = 1;
    public static final double EMOTIONAL_GAP_BASE_MULTIPLIER = 1.0;
    public static final double EMOTIONAL_GAP_MAX_SCORE = 100.0;
    public static final double EMOTIONAL_GAP_SCALING_FACTOR = 0.2;
    public static final double TIME_DECAY_HALF_LIFE_DAYS = 365.25 * 5.0; // 5 years
    public static final double TIME_DECAY_FLOOR = 0.5;

    private static final List<MediaType> MEDIA_TYPES = List.of(
            MediaType.SHOW, MediaType.SEASON, MediaType.EPISODE, MediaType.MOVIE, MediaType.GAME, MediaType.MANGA
    );

    private final UserListRepository userListRepository;
    private final UserListItemRepository userListItemRepository;
    private final UserRatingRepository userRatingRepository;
    private final GlobalRankingRepository globalRankingRepository;

    public PageRankAggregationService(UserListRepository userListRepository,
                                     UserListItemRepository userListItemRepository,
                                     UserRatingRepository userRatingRepository,
                                     GlobalRankingRepository globalRankingRepository) {
        this.userListRepository = userListRepository;
        this.userListItemRepository = userListItemRepository;
        this.userRatingRepository = userRatingRepository;
        this.globalRankingRepository = globalRankingRepository;
    }

    private double calculateEdgeWeight(double gapMultiplier, OffsetDateTime updatedAt) {
        OffsetDateTime now = OffsetDateTime.now();
        double daysOld = Math.max(0.0, (double) ChronoUnit.SECONDS.between(updatedAt, now) / (60.0 * 60.0 * 24.0));
        double decay = Math.pow(0.5, daysOld / TIME_DECAY_HALF_LIFE_DAYS);
        double timeDecay = Math.max(TIME_DECAY_FLOOR, decay);
        return gapMultiplier * timeDecay;
    }

    private record UserListEntry(UUID id, OffsetDateTime updatedAt, List<UserListItemEntity> items) {}

    private record UserEdge(String winner, String loser, double gap, OffsetDateTime updatedAt) {}

    private record ScoredMedia(String mediaId, double score, int appearanceCount) {}

    public void processAllMediaTypes() {
        log.info("Starting Rank Aggregation Engine in parallel across {} media types...", MEDIA_TYPES.size());
        MEDIA_TYPES.parallelStream().forEach(this::processMediaType);
        log.info("Rank Aggregation completed globally.");
    }

    @Transactional
    public void sweepRanks() {
        globalRankingRepository.sweepRanks();
    }

    @Transactional
    public void processMediaType(MediaType mediaType) {
        log.info("--- Processing media_type: {} ---", mediaType);

        Map<String, Map<String, Double>> graph = new HashMap<>();
        Set<String> allMediaIds = new HashSet<>();
        Map<String, Integer> appearanceCount = new HashMap<>();
        Map<UUID, List<UserListEntry>> userLists = new HashMap<>();

        List<UserListEntity> lists = userListRepository.findAll().stream()
                .filter(l -> l.getMediaType() == mediaType)
                .toList();

        for (UserListEntity list : lists) {
            List<UserListItemEntity> items = userListItemRepository.findByListIdOrderByRankPositionAsc(list.getId());
            if (items.isEmpty()) continue;

            userLists.computeIfAbsent(list.getUserId(), k -> new ArrayList<>())
                    .add(new UserListEntry(list.getId(), list.getUpdatedAt(), items));

            for (UserListItemEntity item : items) {
                allMediaIds.add(item.getMediaId());
                appearanceCount.put(item.getMediaId(), appearanceCount.getOrDefault(item.getMediaId(), 0) + 1);
            }
        }

        log.info("Fetched {} lists for {} users.", lists.size(), userLists.size());

        int N = allMediaIds.size();
        if (N == 0) {
            log.info("No data for {}. Skipping...", mediaType);
            return;
        }

        // Batch fetch user ratings for these users
        Map<UUID, Map<String, Integer>> userRatings = new HashMap<>();
        List<UUID> userIds = new ArrayList<>(userLists.keySet());
        for (int i = 0; i < userIds.size(); i += 500) {
            List<UUID> batch = userIds.subList(i, Math.min(i + 500, userIds.size()));
            List<UserRatingEntity> ratings = userRatingRepository.findByUserIdIn(batch);
            for (UserRatingEntity r : ratings) {
                userRatings.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                        .put(r.getMediaId(), r.getScore());
            }
        }

        Map<String, Double> totalLosses = new HashMap<>();
        for (String mId : allMediaIds) totalLosses.put(mId, 0.0);

        // Process per-user deduplication
        for (Map.Entry<UUID, List<UserListEntry>> entry : userLists.entrySet()) {
            UUID userId = entry.getKey();
            List<UserListEntry> uLists = entry.getValue();
            uLists.sort(Comparator.comparing(UserListEntry::updatedAt));

            Map<String, UserEdge> userEdges = new HashMap<>();
            Map<String, Integer> ratingsMap = userRatings.getOrDefault(userId, Map.of());

            for (UserListEntry list : uLists) {
                List<UserListItemEntity> items = list.items();
                for (int i = 0; i < items.size(); i++) {
                    for (int j = i + 1; j < items.size(); j++) {
                        UserListItemEntity winner = items.get(i);
                        UserListItemEntity loser = items.get(j);

                        String pairKey = winner.getMediaId().compareTo(loser.getMediaId()) < 0
                                ? winner.getMediaId() + "|" + loser.getMediaId()
                                : loser.getMediaId() + "|" + winner.getMediaId();

                        Integer winnerScore = ratingsMap.get(winner.getMediaId());
                        Integer loserScore = ratingsMap.get(loser.getMediaId());
                        double gapMultiplier = EMOTIONAL_GAP_BASE_MULTIPLIER;

                        if (winnerScore != null && loserScore != null) {
                            double rawGap = Math.max(0.0, (double) winnerScore - loserScore);
                            gapMultiplier = EMOTIONAL_GAP_BASE_MULTIPLIER +
                                    (rawGap / EMOTIONAL_GAP_MAX_SCORE) * EMOTIONAL_GAP_SCALING_FACTOR;
                        }

                        userEdges.put(pairKey, new UserEdge(winner.getMediaId(), loser.getMediaId(), gapMultiplier, list.updatedAt()));
                    }
                }
            }

            for (UserEdge edge : userEdges.values()) {
                double weight = calculateEdgeWeight(edge.gap(), edge.updatedAt());
                graph.computeIfAbsent(edge.loser(), k -> new HashMap<>())
                        .merge(edge.winner(), weight, Double::sum);

                totalLosses.put(edge.loser(), totalLosses.get(edge.loser()) + weight);
            }
        }

        // Power iteration
        Map<String, Double> scores = new HashMap<>();
        for (String mId : allMediaIds) scores.put(mId, 1.0);

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Map<String, Double> nextScores = new HashMap<>();
            for (String mId : allMediaIds) nextScores.put(mId, 0.0);

            double currentTotalPoints = 0.0;
            for (double s : scores.values()) currentTotalPoints += s;

            double danglingSum = 0.0;

            for (Map.Entry<String, Double> e : scores.entrySet()) {
                String node = e.getKey();
                double score = e.getValue();
                Map<String, Double> outgoing = graph.get(node);
                double nodeLosses = totalLosses.getOrDefault(node, 0.0);

                if (outgoing == null || outgoing.isEmpty() || nodeLosses == 0.0) {
                    danglingSum += score;
                } else {
                    for (Map.Entry<String, Double> out : outgoing.entrySet()) {
                        String winner = out.getKey();
                        double weight = out.getValue();
                        double transfer = (score / nodeLosses) * weight;
                        nextScores.put(winner, nextScores.get(winner) + transfer);
                    }
                }
            }

            double ubi = (UNIVERSAL_BASIC_INCOME_SHARE * currentTotalPoints) / N;
            double danglingShare = (DAMPING_FACTOR * danglingSum) / N;

            double maxDiff = 0.0;
            for (String mId : allMediaIds) {
                double accumulated = nextScores.get(mId);
                double updatedScore = (accumulated * DAMPING_FACTOR) + ubi + danglingShare;
                nextScores.put(mId, updatedScore);

                double diff = Math.abs(updatedScore - scores.getOrDefault(mId, 0.0));
                if (diff > maxDiff) maxDiff = diff;
            }

            scores = nextScores;
            if (maxDiff < CONVERGENCE_THRESHOLD) {
                log.info("Converged at iteration {}", iter + 1);
                break;
            }
        }

        // Calculate Win-Rate Efficiency Score
        List<ScoredMedia> finalScores = new ArrayList<>();
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            int apps = appearanceCount.getOrDefault(e.getKey(), 0);
            double eff = apps > 0 ? (e.getValue() / apps) : 0.0;
            finalScores.add(new ScoredMedia(e.getKey(), eff, apps));
        }

        List<ScoredMedia> qualified = finalScores.stream()
                .filter(i -> i.appearanceCount() >= MIN_LIST_APPEARANCES)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();

        List<ScoredMedia> unqualified = finalScores.stream()
                .filter(i -> i.appearanceCount() < MIN_LIST_APPEARANCES)
                .toList();

        List<GlobalRankingEntity> toSave = new ArrayList<>();

        for (int i = 0; i < qualified.size(); i++) {
            ScoredMedia item = qualified.get(i);
            int rank = i + 1;
            double scaledScore = 1200.0 + Math.log10(item.score() * N) * 400.0;
            double finalElo = Double.isNaN(scaledScore) ? 1200.0 : scaledScore;

            GlobalRankingEntity entity = globalRankingRepository.findById(item.mediaId()).orElseGet(() -> {
                GlobalRankingEntity ge = new GlobalRankingEntity();
                ge.setMediaId(item.mediaId());
                return ge;
            });
            entity.setMediaType(mediaType);
            entity.setEloScore(finalElo);
            entity.setRank(rank);
            toSave.add(entity);
        }

        for (ScoredMedia item : unqualified) {
            double scaledScore = 1200.0 + Math.log10(item.score() * N) * 400.0;
            double finalElo = Double.isNaN(scaledScore) ? 1200.0 : scaledScore;

            GlobalRankingEntity entity = globalRankingRepository.findById(item.mediaId()).orElseGet(() -> {
                GlobalRankingEntity ge = new GlobalRankingEntity();
                ge.setMediaId(item.mediaId());
                return ge;
            });
            entity.setMediaType(mediaType);
            entity.setEloScore(finalElo);
            entity.setRank(null);
            toSave.add(entity);
        }

        globalRankingRepository.saveAll(toSave);
        log.info("Finished writing back DB for {}.", mediaType);
    }
}
