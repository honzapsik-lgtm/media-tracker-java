package com.mediatracker.repository;

import com.mediatracker.model.entity.UserBadgeEntity;
import com.mediatracker.model.entity.UserBadgeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadgeEntity, UserBadgeId> {
    List<UserBadgeEntity> findByUserId(UUID userId);
    long countByUserId(UUID userId);
}
