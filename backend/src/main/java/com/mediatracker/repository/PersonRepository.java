package com.mediatracker.repository;

import com.mediatracker.model.entity.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, String> {
    Optional<PersonEntity> findByTmdbId(Integer tmdbId);
    Optional<PersonEntity> findByAnilistId(Integer anilistId);
    Optional<PersonEntity> findByIgdbId(Integer igdbId);
    Optional<PersonEntity> findByMalId(Integer malId);
    Optional<PersonEntity> findByRawgId(Integer rawgId);
    Optional<PersonEntity> findByMangadexId(String mangadexId);
}
