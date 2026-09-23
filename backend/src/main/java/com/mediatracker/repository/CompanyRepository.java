package com.mediatracker.repository;

import com.mediatracker.model.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, String> {
    Optional<CompanyEntity> findByTmdbId(Integer tmdbId);
    Optional<CompanyEntity> findByAnilistId(Integer anilistId);
    Optional<CompanyEntity> findByIgdbId(Integer igdbId);
    Optional<CompanyEntity> findByTmdbNetworkId(Integer tmdbNetworkId);
}
