package com.mediatracker.service;

import com.mediatracker.model.entity.FriendshipEntity;
import com.mediatracker.model.entity.UserEntity;
import com.mediatracker.model.entity.UserFriendPreferenceEntity;
import com.mediatracker.model.enums.FriendshipStatus;
import com.mediatracker.repository.FriendshipRepository;
import com.mediatracker.repository.UserFriendPreferenceRepository;
import com.mediatracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserFriendPreferenceRepository userFriendPreferenceRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserFriendPreferenceRepository userFriendPreferenceRepository,
                             UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userFriendPreferenceRepository = userFriendPreferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFriendsOverview(UUID currentUserId) {
        List<FriendshipEntity> accepted = friendshipRepository.findAcceptedFriendships(currentUserId);
        List<UserFriendPreferenceEntity> prefs = userFriendPreferenceRepository.findByUserId(currentUserId);

        Map<UUID, UserFriendPreferenceEntity> prefMap = new HashMap<>();
        for (UserFriendPreferenceEntity p : prefs) {
            prefMap.put(p.getFriendId(), p);
        }

        Set<UUID> friendIds = new HashSet<>();
        for (FriendshipEntity f : accepted) {
            friendIds.add(f.getSenderId().equals(currentUserId) ? f.getReceiverId() : f.getSenderId());
        }

        Map<UUID, UserEntity> userMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            for (UserEntity u : userRepository.findAllById(friendIds)) {
                userMap.put(u.getId(), u);
            }
        }

        List<Map<String, Object>> friends = new ArrayList<>();
        for (FriendshipEntity f : accepted) {
            UUID friendId = f.getSenderId().equals(currentUserId) ? f.getReceiverId() : f.getSenderId();
            UserEntity u = userMap.get(friendId);
            if (u != null) {
                UserFriendPreferenceEntity p = prefMap.get(friendId);
                Map<String, Object> fMap = new HashMap<>();
                fMap.put("id", u.getId());
                fMap.put("name", u.getName());
                fMap.put("username", u.getUsername());
                fMap.put("image", u.getImage());
                fMap.put("friendshipId", f.getId());
                fMap.put("since", f.getUpdatedAt());
                fMap.put("hide_activity", p != null && p.isHideActivity());
                fMap.put("hide_ratings", p != null && p.isHideRatings());
                friends.add(fMap);
            }
        }

        // Pending received
        List<FriendshipEntity> pendingRec = friendshipRepository.findByReceiverIdAndStatus(currentUserId, FriendshipStatus.PENDING);
        List<Map<String, Object>> pendingReceived = new ArrayList<>();
        for (FriendshipEntity f : pendingRec) {
            userRepository.findById(f.getSenderId()).ifPresent(sender -> {
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("friendshipId", f.getId());
                rMap.put("created_at", f.getCreatedAt());
                rMap.put("sender", Map.of(
                        "id", sender.getId(),
                        "name", sender.getName() != null ? sender.getName() : "",
                        "username", sender.getUsername() != null ? sender.getUsername() : "",
                        "image", sender.getImage() != null ? sender.getImage() : ""
                ));
                pendingReceived.add(rMap);
            });
        }

        // Pending sent
        List<FriendshipEntity> pendingS = friendshipRepository.findBySenderIdAndStatus(currentUserId, FriendshipStatus.PENDING);
        List<Map<String, Object>> pendingSent = new ArrayList<>();
        for (FriendshipEntity f : pendingS) {
            userRepository.findById(f.getReceiverId()).ifPresent(receiver -> {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("friendshipId", f.getId());
                sMap.put("created_at", f.getCreatedAt());
                sMap.put("receiver", Map.of(
                        "id", receiver.getId(),
                        "name", receiver.getName() != null ? receiver.getName() : "",
                        "username", receiver.getUsername() != null ? receiver.getUsername() : "",
                        "image", receiver.getImage() != null ? receiver.getImage() : ""
                ));
                pendingSent.add(sMap);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("friends", friends);
        response.put("pendingReceived", pendingReceived);
        response.put("pendingSent", pendingSent);
        return response;
    }

    @Transactional
    public FriendshipEntity sendFriendRequest(UUID currentUserId, String target) {
        String cleanTarget = target.trim();
        Optional<UserEntity> targetUser = userRepository.findByUsernameIgnoreCase(cleanTarget);
        if (targetUser.isEmpty()) {
            targetUser = userRepository.findByEmail(cleanTarget);
        }
        if (targetUser.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + cleanTarget);
        }

        UUID targetId = targetUser.get().getId();
        if (targetId.equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        Optional<FriendshipEntity> existing = friendshipRepository.findBetweenUsers(currentUserId, targetId);
        if (existing.isPresent()) {
            FriendshipEntity f = existing.get();
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalStateException("Already friends");
            }
            if (f.getStatus() == FriendshipStatus.PENDING) {
                throw new IllegalStateException("Friend request already pending");
            }
            if (f.getStatus() == FriendshipStatus.BLOCKED) {
                throw new IllegalStateException("Cannot send friend request");
            }
            f.setStatus(FriendshipStatus.PENDING);
            f.setSenderId(currentUserId);
            f.setReceiverId(targetId);
            f.setUpdatedAt(OffsetDateTime.now());
            return friendshipRepository.save(f);
        }

        FriendshipEntity f = new FriendshipEntity();
        f.setSenderId(currentUserId);
        f.setReceiverId(targetId);
        f.setStatus(FriendshipStatus.PENDING);
        f.setCreatedAt(OffsetDateTime.now());
        f.setUpdatedAt(OffsetDateTime.now());
        return friendshipRepository.save(f);
    }

    @Transactional
    public boolean respondToRequest(UUID currentUserId, UUID friendshipId, String action) {
        Optional<FriendshipEntity> fOpt = friendshipRepository.findById(friendshipId);
        if (fOpt.isEmpty()) return false;
        FriendshipEntity f = fOpt.get();

        if ("accept".equalsIgnoreCase(action)) {
            if (!f.getReceiverId().equals(currentUserId)) return false;
            f.setStatus(FriendshipStatus.ACCEPTED);
            f.setUpdatedAt(OffsetDateTime.now());
            friendshipRepository.save(f);
            return true;
        } else if ("decline".equalsIgnoreCase(action)) {
            if (!f.getReceiverId().equals(currentUserId)) return false;
            friendshipRepository.delete(f);
            return true;
        } else if ("block".equalsIgnoreCase(action)) {
            f.setStatus(FriendshipStatus.BLOCKED);
            f.setUpdatedAt(OffsetDateTime.now());
            friendshipRepository.save(f);
            return true;
        }

        return false;
    }

    @Transactional
    public boolean removeFriend(UUID currentUserId, UUID friendshipId) {
        Optional<FriendshipEntity> fOpt = friendshipRepository.findById(friendshipId);
        if (fOpt.isEmpty()) return false;
        FriendshipEntity f = fOpt.get();

        if (f.getSenderId().equals(currentUserId) || f.getReceiverId().equals(currentUserId)) {
            friendshipRepository.delete(f);
            return true;
        }
        return false;
    }

    @Transactional
    public UserFriendPreferenceEntity updatePreferences(UUID currentUserId, UUID friendId, Boolean hideActivity, Boolean hideRatings) {
        UserFriendPreferenceEntity pref = userFriendPreferenceRepository.findByUserIdAndFriendId(currentUserId, friendId)
                .orElseGet(() -> {
                    UserFriendPreferenceEntity p = new UserFriendPreferenceEntity();
                    p.setUserId(currentUserId);
                    p.setFriendId(friendId);
                    return p;
                });

        if (hideActivity != null) pref.setHideActivity(hideActivity);
        if (hideRatings != null) pref.setHideRatings(hideRatings);
        pref.setUpdatedAt(OffsetDateTime.now());
        return userFriendPreferenceRepository.save(pref);
    }
}
