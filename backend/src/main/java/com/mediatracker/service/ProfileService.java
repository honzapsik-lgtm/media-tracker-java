package com.mediatracker.service;

import com.mediatracker.model.entity.*;
import com.mediatracker.model.enums.RatingStyle;
import com.mediatracker.model.enums.VisibilityLevel;
import com.mediatracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ProfileService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{3,25}$");

    private final UserRepository userRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserStatsCacheRepository userStatsCacheRepository;
    private final UserPrivacySettingsRepository userPrivacySettingsRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserActivityRepository userActivityRepository;
    private final StatsService statsService;

    public ProfileService(UserRepository userRepository,
                          UserBadgeRepository userBadgeRepository,
                          UserStatsCacheRepository userStatsCacheRepository,
                          UserPrivacySettingsRepository userPrivacySettingsRepository,
                          FriendshipRepository friendshipRepository,
                          UserActivityRepository userActivityRepository,
                          StatsService statsService) {
        this.statsService = statsService;
        this.userRepository = userRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userStatsCacheRepository = userStatsCacheRepository;
        this.userPrivacySettingsRepository = userPrivacySettingsRepository;
        this.friendshipRepository = friendshipRepository;
        this.userActivityRepository = userActivityRepository;
    }

    /** SELF, FRIENDS, PENDING_SENT, PENDING_RECEIVED or NONE, seen from the viewer. */
    private String relation(UUID viewerId, UUID userId) {
        if (viewerId == null) return "NONE";
        if (viewerId.equals(userId)) return "SELF";
        return friendshipRepository.findBetweenUsers(viewerId, userId).map(f -> switch (f.getStatus()) {
            case ACCEPTED -> "FRIENDS";
            case PENDING -> f.getSenderId().equals(viewerId) ? "PENDING_SENT" : "PENDING_RECEIVED";
            default -> "NONE";
        }).orElse("NONE");
    }

    private static boolean canView(VisibilityLevel level, String relation) {
        if ("SELF".equals(relation)) return true;
        if (level == null || level == VisibilityLevel.PUBLIC) return true;
        return level == VisibilityLevel.FRIENDS_ONLY && "FRIENDS".equals(relation);
    }

    private UserPrivacySettingsEntity privacyOf(UUID userId) {
        return userPrivacySettingsRepository.findByUserId(userId).orElseGet(() -> {
            UserPrivacySettingsEntity ps = new UserPrivacySettingsEntity();
            ps.setUserId(userId);
            return ps;
        });
    }

    /** A user's own activity for their profile page, if their activity privacy lets the viewer see it. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getProfileActivity(String username, UUID viewerUserId, int limit) {
        Optional<UserEntity> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isEmpty()) return Optional.empty();
        UserEntity user = userOpt.get();

        String rel = relation(viewerUserId, user.getId());
        boolean allowed = canView(privacyOf(user.getId()).getActivityVisibility(), rel);

        List<Map<String, Object>> activities = new ArrayList<>();
        if (allowed) {
            var page = userActivityRepository.findByUserId(user.getId(),
                    org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(limit, 200)),
                            org.springframework.data.domain.Sort.by("createdAt").descending()));
            for (UserActivityEntity act : page.getContent()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", act.getId());
                map.put("type", act.getType());
                map.put("media_id", act.getMediaId());
                map.put("media_title", act.getMediaTitle());
                map.put("media_image", act.getMediaImage());
                map.put("media_type", act.getMediaType());
                map.put("data", act.getData());
                map.put("created_at", act.getCreatedAt());
                activities.add(map);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("canView", allowed);
        result.put("activities", activities);
        return Optional.of(result);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getProfile(String username, UUID viewerUserId) {
        Optional<UserEntity> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isEmpty()) return Optional.empty();

        UserEntity user = userOpt.get();
        UUID userId = user.getId();

        UserPrivacySettingsEntity privacy = privacyOf(userId);

        boolean isOwner = viewerUserId != null && viewerUserId.equals(userId);
        String rel = relation(viewerUserId, userId);

        List<UserBadgeEntity> badges = userBadgeRepository.findByUserId(userId);
        List<String> badgeIds = badges.stream().map(UserBadgeEntity::getBadgeId).toList();

        List<UserStatsCacheEntity> stats = userStatsCacheRepository.findByUserId(userId);
        Map<String, Object> statsMap = new HashMap<>();
        for (UserStatsCacheEntity s : stats) {
            statsMap.put(s.getMediaType().name().toLowerCase(), s.getStatsJson());
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("name", user.getName());
        profile.put("image", user.getImage());
        profile.put("role", user.getRole());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("realName", user.getRealName());
        profile.put("stateRegion", user.getStateRegion());
        profile.put("country", user.getCountry());
        profile.put("showcaseBadges", user.getShowcaseBadges() != null ? Arrays.asList(user.getShowcaseBadges()) : List.of());
        profile.put("badges", badgeIds);
        profile.put("stats", statsMap);
        profile.put("privacy", privacy);
        profile.put("isOwner", isOwner);
        if (isOwner) profile.put("badgeProgress", statsService.badgeProgress(userId));
        if (canView(privacy.getRatingsVisibility(), rel)) profile.put("insights", statsService.profileInsights(userId));
        profile.put("relation", rel);
        profile.put("canViewProfile", canView(privacy.getProfileVisibility(), rel));
        profile.put("canViewRatings", canView(privacy.getRatingsVisibility(), rel));
        profile.put("canViewActivity", canView(privacy.getActivityVisibility(), rel));

        return Optional.of(profile);
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username, UUID currentUserId) {
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            return false;
        }
        Optional<UserEntity> existing = userRepository.findByUsernameIgnoreCase(username.trim());
        if (existing.isEmpty()) return true;
        return currentUserId != null && existing.get().getId().equals(currentUserId);
    }

    @Transactional
    public UserEntity updateUsername(UUID currentUserId, String newUsername) {
        String clean = newUsername != null ? newUsername.trim() : "";
        if (!USERNAME_PATTERN.matcher(clean).matches()) {
            throw new IllegalArgumentException("Username must be 3-25 characters and alphanumeric, underscore, hyphen or dot.");
        }

        Optional<UserEntity> existing = userRepository.findByUsernameIgnoreCase(clean);
        if (existing.isPresent() && !existing.get().getId().equals(currentUserId)) {
            throw new IllegalStateException("Username is already taken.");
        }

        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setUsername(clean);
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity updateProfile(UUID currentUserId, String realName, String stateRegion, String country, List<String> showcaseBadges) {
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (realName != null) user.setRealName(realName);
        if (stateRegion != null) user.setStateRegion(stateRegion);
        if (country != null) user.setCountry(country);
        if (showcaseBadges != null) user.setShowcaseBadges(showcaseBadges.toArray(new String[0]));

        return userRepository.save(user);
    }

    public RatingStyle getRatingStyle(UUID currentUserId) {
        return userRepository.findById(currentUserId)
                .map(UserEntity::getRatingStyle)
                .orElse(RatingStyle.PERCENT);
    }

    @Transactional
    public RatingStyle updateRatingStyle(UUID currentUserId, RatingStyle ratingStyle) {
        if (ratingStyle == null) throw new IllegalArgumentException("ratingStyle is required");
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRatingStyle(ratingStyle);
        return userRepository.save(user).getRatingStyle();
    }

    public UserPrivacySettingsEntity getPrivacySettings(UUID currentUserId) {
        return userPrivacySettingsRepository.findByUserId(currentUserId).orElseGet(() -> {
            UserPrivacySettingsEntity ps = new UserPrivacySettingsEntity();
            ps.setUserId(currentUserId);
            return userPrivacySettingsRepository.save(ps);
        });
    }

    @Transactional
    public UserPrivacySettingsEntity updatePrivacySettings(UUID currentUserId,
                                                           VisibilityLevel profile,
                                                           VisibilityLevel ratings,
                                                           VisibilityLevel watchlist,
                                                           VisibilityLevel activity) {
        UserPrivacySettingsEntity ps = getPrivacySettings(currentUserId);
        if (profile != null) ps.setProfileVisibility(profile);
        if (ratings != null) ps.setRatingsVisibility(ratings);
        if (watchlist != null) ps.setWatchlistVisibility(watchlist);
        if (activity != null) ps.setActivityVisibility(activity);
        ps.setUpdatedAt(OffsetDateTime.now());
        return userPrivacySettingsRepository.save(ps);
    }
}
