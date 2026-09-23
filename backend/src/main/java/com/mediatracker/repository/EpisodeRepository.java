package com.mediatracker.repository;

import com.mediatracker.model.entity.EpisodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<EpisodeEntity, String> {
    List<EpisodeEntity> findByMediaId(String mediaId);
    Optional<EpisodeEntity> findByAnilistId(Integer anilistId);
    Optional<EpisodeEntity> findByTmdbId(Integer tmdbId);
}
