package com.mediatracker.repository;

import com.mediatracker.model.entity.MediaStatsEntity;
import com.mediatracker.model.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaStatsRepository extends JpaRepository<MediaStatsEntity, String> {
    List<MediaStatsEntity> findByMediaType(MediaType mediaType);
}
