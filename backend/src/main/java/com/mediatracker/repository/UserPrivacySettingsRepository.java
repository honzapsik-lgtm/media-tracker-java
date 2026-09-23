package com.mediatracker.repository;

import com.mediatracker.model.entity.UserPrivacySettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettingsEntity, UUID> {
    Optional<UserPrivacySettingsEntity> findByUserId(UUID userId);
    List<UserPrivacySettingsEntity> findByUserIdIn(List<UUID> userIds);
}
