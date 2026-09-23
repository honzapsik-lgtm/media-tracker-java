package com.mediatracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.entity.FriendshipEntity;
import com.mediatracker.model.entity.UserEntity;
import com.mediatracker.model.entity.UserFriendPreferenceEntity;
import com.mediatracker.model.entity.UserRatingEntity;
import com.mediatracker.model.enums.ActivityType;
import com.mediatracker.model.enums.FriendshipStatus;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.repository.*;
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
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final UserRatingRepository userRatingRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserFriendPreferenceRepository userFriendPreferenceRepository;
    private final StatsService statsService;
    private final ActivityService activityService;
    private final ObjectMapper objectMapper;

    public RatingService(UserRatingRepository userRatingRepository,
                         UserRepository userRepository,
                         FriendshipRepository friendshipRepository,
                         UserFriendPreferenceRepository userFriendPreferenceRepository,
                         StatsService statsService,
                         ActivityService activityService,
                         ObjectMapper objectMapper) {
        this.userRatingRepository = userRatingRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.userFriendPreferenceRepository = userFriendPreferenceRepository;
        this.statsService = statsService;
        this.activityService = activityService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserRatingEntity saveRating(UUID userId,
                                      String mediaId,
                                      Integer score,
                                      Boolean isDeepReview,
                                      Map<String, Object> criteriaScores,
                                      String reviewText,
                                      String mediaTitle,
                                      String mediaImage,
                                      String mediaReleaseDate) {
        if (score == null || mediaId == null) {
            throw new IllegalArgumentException("mediaId and score are required");
        }

        UserEntity user = userRepository.findById(userId).orElse(null);
        String username = user != null && user.getUsername() != null ? user.getUsername()
                : user != null && user.getName() != null ? user.getName()
                : "Anonymous";
        String avatarUrl = user != null ? user.getImage() : null;

        UserRatingEntity rating = userRatingRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseGet(() -> {
                    UserRatingEntity r = new UserRatingEntity();
                    r.setUserId(userId);
                    r.setMediaId(mediaId);
                    return r;
                });

        rating.setScore(score);
        rating.setIsDeepReview(Boolean.TRUE.equals(isDeepReview));
        if (Boolean.TRUE.equals(isDeepReview) && criteriaScores != null) {
            rating.setCriteriaScores(objectMapper.valueToTree(criteriaScores));
        }
        if (reviewText != null) rating.setReviewText(reviewText);
        if (mediaTitle != null) rating.setMediaTitle(mediaTitle);
        if (mediaImage != null) rating.setMediaImage(mediaImage);
        if (mediaReleaseDate != null) rating.setMediaReleaseDate(mediaReleaseDate);
        rating.setUsername(username);
        rating.setAvatarUrl(avatarUrl);
        rating.setCreatedAt(OffsetDateTime.now());

        UserRatingEntity saved = userRatingRepository.save(rating);

        MediaType mediaType = statsService.inferMediaType(mediaId);
        statsService.refreshMediaStats(mediaId, mediaType);
        statsService.updateUserStatsCache(userId, mediaType);
        statsService.awardBadges(userId);

        Map<String, Object> activityData = new HashMap<>();
        activityData.put("score", score);
        if (reviewText != null && !reviewText.isBlank()) {
            activityData.put("hasReview", true);
        }
        activityService.logActivity(userId, ActivityType.RATED_MEDIA, mediaId, mediaTitle, mediaImage, mediaType, activityData);

        return saved;
    }

    @Transactional
    public boolean deleteRating(UUID userId, String mediaId) {
        Optional<UserRatingEntity> existing = userRatingRepository.findByUserIdAndMediaId(userId, mediaId);
        if (existing.isPresent()) {
            userRatingRepository.delete(existing.get());
            MediaType mediaType = statsService.inferMediaType(mediaId);
            statsService.refreshMediaStats(mediaId, mediaType);
            statsService.updateUserStatsCache(userId, mediaType);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMediaRatingData(UUID userId, String mediaId) {
        Map<String, Object> result = new HashMap<>();

        if (userId != null) {
            userRatingRepository.findByUserIdAndMediaId(userId, mediaId).ifPresent(r -> result.put("personal", r));
        }

        List<UserRatingEntity> deepReviews = userRatingRepository.findByMediaIdAndIsDeepReviewTrue(mediaId);
        result.put("globalCriteriaAverages", statsService.calculateCriteriaAverages(deepReviews));

        statsService.getMediaStats(mediaId).ifPresent(s -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("community_average", s.getCommunityAverage() != null ? s.getCommunityAverage().intValue() : 0);
            stats.put("total_ratings", s.getTotalRatings() != null ? s.getTotalRatings() : 0);
            result.put("stats", stats);
        });

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getPrefixRatings(UUID userId, String prefix) {
        if (userId == null || prefix == null) return Map.of();
        List<UserRatingEntity> list = userRatingRepository.findByUserIdAndMediaIdStartingWith(userId, prefix);
        Map<String, Integer> map = new HashMap<>();
        for (UserRatingEntity r : list) {
            map.put(r.getMediaId(), r.getScore());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<UserRatingEntity> getFriendRatings(UUID currentUserId, String mediaId) {
        if (currentUserId == null || mediaId == null) return List.of();

        List<FriendshipEntity> friendships = friendshipRepository.findAcceptedFriendships(currentUserId);
        List<UUID> friendIds = new ArrayList<>();
        for (FriendshipEntity f : friendships) {
            UUID friendId = f.getSenderId().equals(currentUserId) ? f.getReceiverId() : f.getSenderId();
            friendIds.add(friendId);
        }

        if (friendIds.isEmpty()) return List.of();

        // Check if current user hid ratings from any friend
        List<UserFriendPreferenceEntity> prefs = userFriendPreferenceRepository.findByUserId(currentUserId);
        Set<UUID> mutedRatings = new HashSet<>();
        for (UserFriendPreferenceEntity p : prefs) {
            if (p.isHideRatings()) mutedRatings.add(p.getFriendId());
        }
        friendIds.removeIf(mutedRatings::contains);

        if (friendIds.isEmpty()) return List.of();

        return userRatingRepository.findByUserIdInAndMediaId(friendIds, mediaId);
    }

    @Transactional(readOnly = true)
    public Page<UserRatingEntity> getUserRatings(UUID userId, int page, int limit) {
        return userRatingRepository.findByUserId(userId, PageRequest.of(page - 1, limit, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public Page<UserRatingEntity> getUserReviews(UUID userId, int page, int limit) {
        return userRatingRepository.findByUserIdAndReviewTextIsNotNull(userId, PageRequest.of(page - 1, limit, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public Page<UserRatingEntity> getUserRankings(UUID userId, String type, int page, int limit) {
        return userRatingRepository.findUserRankings(userId, type != null ? type.toLowerCase() : "show", PageRequest.of(page - 1, limit));
    }
}
