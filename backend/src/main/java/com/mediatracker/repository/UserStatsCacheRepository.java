package com.mediatracker.repository;

import com.mediatracker.model.entity.UserStatsCacheEntity;
import com.mediatracker.model.entity.UserStatsCacheId;
import com.mediatracker.model.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserStatsCacheRepository extends JpaRepository<UserStatsCacheEntity, UserStatsCacheId> {
    Optional<UserStatsCacheEntity> findByUserIdAndMediaType(UUID userId, MediaType mediaType);
    List<UserStatsCacheEntity> findByUserId(UUID userId);
}
