package com.mediatracker.repository;

import com.mediatracker.model.entity.GlobalRankingEntity;
import com.mediatracker.model.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalRankingRepository extends JpaRepository<GlobalRankingEntity, String> {
    List<GlobalRankingEntity> findByMediaTypeOrderByEloScoreDesc(MediaType mediaType);
    List<GlobalRankingEntity> findByMediaTypeOrderByRankAsc(MediaType mediaType);

    @Modifying
    @Query(value = """
        WITH Ranked AS (
          SELECT media_id, RANK() OVER (PARTITION BY media_type ORDER BY elo_score DESC) as new_rank
          FROM global_rankings
        )
        UPDATE global_rankings
        SET rank = Ranked.new_rank
        FROM Ranked
        WHERE global_rankings.media_id = Ranked.media_id
    """, nativeQuery = true)
    void sweepRanks();
}
