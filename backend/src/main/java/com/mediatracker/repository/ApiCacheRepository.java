package com.mediatracker.repository;

import com.mediatracker.model.entity.ApiCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApiCacheRepository extends JpaRepository<ApiCacheEntity, String> {
    Optional<ApiCacheEntity> findByIdAndExpiresAtAfter(String id, LocalDateTime now);
    void deleteByExpiresAtBefore(LocalDateTime now);
}
