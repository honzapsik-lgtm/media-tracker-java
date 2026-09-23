package com.mediatracker.repository;

import com.mediatracker.model.entity.UserFriendPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFriendPreferenceRepository extends JpaRepository<UserFriendPreferenceEntity, UUID> {
    List<UserFriendPreferenceEntity> findByUserId(UUID userId);
    Optional<UserFriendPreferenceEntity> findByUserIdAndFriendId(UUID userId, UUID friendId);
}
