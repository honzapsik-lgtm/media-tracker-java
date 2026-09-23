package com.mediatracker.service;

import com.mediatracker.model.entity.*;
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

    public ProfileService(UserRepository userRepository,
                          UserBadgeRepository userBadgeRepository,
                          UserStatsCacheRepository userStatsCacheRepository,
                          UserPrivacySettingsRepository userPrivacySettingsRepository) {
        this.userRepository = userRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userStatsCacheRepository = userStatsCacheRepository;
        this.userPrivacySettingsRepository = userPrivacySettingsRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getProfile(String username, UUID viewerUserId) {
        Optional<UserEntity> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isEmpty()) return Optional.empty();

        UserEntity user = userOpt.get();
        UUID userId = user.getId();

        UserPrivacySettingsEntity privacy = userPrivacySettingsRepository.findByUserId(userId).orElseGet(() -> {
            UserPrivacySettingsEntity ps = new UserPrivacySettingsEntity();
            ps.setUserId(userId);
            return ps;
        });

        boolean isOwner = viewerUserId != null && viewerUserId.equals(userId);

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
