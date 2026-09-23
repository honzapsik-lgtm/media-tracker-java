package com.mediatracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.entity.*;
import com.mediatracker.model.enums.ActivityType;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.model.enums.VisibilityLevel;
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
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final UserActivityRepository userActivityRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserFriendPreferenceRepository userFriendPreferenceRepository;
    private final UserPrivacySettingsRepository userPrivacySettingsRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ActivityService(UserActivityRepository userActivityRepository,
                           FriendshipRepository friendshipRepository,
                           UserFriendPreferenceRepository userFriendPreferenceRepository,
                           UserPrivacySettingsRepository userPrivacySettingsRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.userActivityRepository = userActivityRepository;
        this.friendshipRepository = friendshipRepository;
        this.userFriendPreferenceRepository = userFriendPreferenceRepository;
        this.userPrivacySettingsRepository = userPrivacySettingsRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserActivityEntity logActivity(UUID userId,
                                          ActivityType type,
                                          String mediaId,
                                          String mediaTitle,
                                          String mediaImage,
                                          MediaType mediaType,
                                          Map<String, Object> data) {
        try {
            UserActivityEntity activity = new UserActivityEntity();
            activity.setUserId(userId);
            activity.setType(type);
            activity.setMediaId(mediaId);
            activity.setMediaTitle(mediaTitle);
            activity.setMediaImage(mediaImage);
            activity.setMediaType(mediaType);
            activity.setData(objectMapper.valueToTree(data != null ? data : Map.of()));
            activity.setCreatedAt(OffsetDateTime.now());

            return userActivityRepository.save(activity);
        } catch (Exception e) {
            log.error("Failed to log activity for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getActivityFeed(UUID currentUserId, String scope, int page, int limit) {
        if (currentUserId == null) return List.of();

        // 1. Get accepted friendships
        List<FriendshipEntity> friendships = friendshipRepository.findAcceptedFriendships(currentUserId);
        List<UUID> friendIds = new ArrayList<>();
        for (FriendshipEntity f : friendships) {
            UUID friendId = f.getSenderId().equals(currentUserId) ? f.getReceiverId() : f.getSenderId();
            friendIds.add(friendId);
        }

        // 2. Filter out friends muted by current user (hide_activity)
        List<UserFriendPreferenceEntity> prefs = userFriendPreferenceRepository.findByUserId(currentUserId);
        Set<UUID> muted = new HashSet<>();
        for (UserFriendPreferenceEntity p : prefs) {
            if (p.isHideActivity()) muted.add(p.getFriendId());
        }
        friendIds.removeIf(muted::contains);

        // 3. Filter out friends whose privacy settings hide activity (PRIVATE)
        List<UserPrivacySettingsEntity> privacies = userPrivacySettingsRepository.findByUserIdIn(friendIds);
        Set<UUID> privates = new HashSet<>();
        for (UserPrivacySettingsEntity ps : privacies) {
            if (ps.getActivityVisibility() == VisibilityLevel.PRIVATE) {
                privates.add(ps.getUserId());
            }
        }
        friendIds.removeIf(privates::contains);

        List<UUID> targetUserIds = new ArrayList<>();
        if ("self".equalsIgnoreCase(scope)) {
            targetUserIds.add(currentUserId);
        } else if ("friends".equalsIgnoreCase(scope)) {
            targetUserIds.addAll(friendIds);
        } else {
            // "all"
            targetUserIds.add(currentUserId);
            targetUserIds.addAll(friendIds);
        }

        if (targetUserIds.isEmpty()) return List.of();

        Page<UserActivityEntity> activitiesPage = userActivityRepository.findByUserIdIn(
                targetUserIds,
                PageRequest.of(page - 1, limit, Sort.by("createdAt").descending())
        );

        // Fetch users map for author enrichment
        List<UserEntity> users = userRepository.findAllById(targetUserIds);
        Map<UUID, UserEntity> userMap = new HashMap<>();
        for (UserEntity u : users) userMap.put(u.getId(), u);

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserActivityEntity act : activitiesPage.getContent()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", act.getId());
            map.put("user_id", act.getUserId());
            map.put("type", act.getType());
            map.put("media_id", act.getMediaId());
            map.put("media_title", act.getMediaTitle());
            map.put("media_image", act.getMediaImage());
            map.put("media_type", act.getMediaType());
            map.put("data", act.getData());
            map.put("created_at", act.getCreatedAt());

            UserEntity u = userMap.get(act.getUserId());
            if (u != null) {
                Map<String, Object> uMap = new HashMap<>();
                uMap.put("id", u.getId());
                uMap.put("name", u.getName());
                uMap.put("username", u.getUsername());
                uMap.put("image", u.getImage());
                map.put("user", uMap);
            }
            result.add(map);
        }

        return result;
    }
}
