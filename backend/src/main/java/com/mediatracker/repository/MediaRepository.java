package com.mediatracker.repository;

import com.mediatracker.model.entity.MediaEntity;
import com.mediatracker.model.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, String> {
    Optional<MediaEntity> findByAnilistId(Integer anilistId);
    Optional<MediaEntity> findByTmdbId(Integer tmdbId);
    Optional<MediaEntity> findByIgdbId(Integer igdbId);
    Optional<MediaEntity> findByMangadexId(String mangadexId);
    Optional<MediaEntity> findByMalId(Integer malId);
    List<MediaEntity> findByType(MediaType type);
    List<MediaEntity> findByRelatedMediaId(String relatedMediaId);
}
