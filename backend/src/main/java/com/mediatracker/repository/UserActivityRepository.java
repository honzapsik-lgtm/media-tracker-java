package com.mediatracker.repository;

import com.mediatracker.model.entity.UserActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivityEntity, UUID> {
    List<UserActivityEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Page<UserActivityEntity> findByUserIdIn(List<UUID> userIds, Pageable pageable);
    Page<UserActivityEntity> findByUserId(UUID userId, Pageable pageable);
}
