package com.mediatracker.repository;

import com.mediatracker.model.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<SeasonEntity, String> {
    List<SeasonEntity> findByMediaId(String mediaId);
    Optional<SeasonEntity> findByAnilistId(Integer anilistId);
    Optional<SeasonEntity> findByTmdbId(Integer tmdbId);
}
