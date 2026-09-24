package com.mediatracker.repository;

import com.mediatracker.model.entity.ApiCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface ApiCacheRepository extends JpaRepository<ApiCacheEntity, String> {
    Optional<ApiCacheEntity> findByIdAndExpiresAtAfter(String id, LocalDateTime now);
    void deleteByExpiresAtBefore(LocalDateTime now);
    long countByExpiresAtBefore(LocalDateTime now);
    long countByExpiresAtAfter(LocalDateTime now);

    @Query("select c.id from ApiCacheEntity c where c.id like concat(:prefix, '%')")
    List<String> findIdsWithPrefix(@Param("prefix") String prefix);

    @Modifying
    @Query("delete from ApiCacheEntity c where c.id in :ids")
    int deleteSelectedIds(@Param("ids") Collection<String> ids);
}
