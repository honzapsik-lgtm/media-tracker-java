package com.mediatracker.repository;

import com.mediatracker.model.entity.UserRatingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRatingEntity, UUID> {
    Optional<UserRatingEntity> findByUserIdAndMediaId(UUID userId, String mediaId);
    List<UserRatingEntity> findByUserId(UUID userId);
    List<UserRatingEntity> findByMediaId(String mediaId);
    List<UserRatingEntity> findByMediaIdAndIsDeepReviewTrue(String mediaId);
    List<UserRatingEntity> findByUserIdInAndMediaId(List<UUID> userIds, String mediaId);
    List<UserRatingEntity> findByUserIdIn(List<UUID> userIds);
    List<UserRatingEntity> findByUserIdAndMediaIdStartingWith(UUID userId, String prefix);
    List<UserRatingEntity> findByUserIdAndMediaIdIn(UUID userId, List<String> mediaIds);
    List<UserRatingEntity> findByMediaIdAndReviewTextIsNotNullOrderByCreatedAtDesc(String mediaId);

    Page<UserRatingEntity> findByUserId(UUID userId, Pageable pageable);
    Page<UserRatingEntity> findByUserIdAndReviewTextIsNotNull(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
    long countByUserIdAndReviewTextIsNotNull(UUID userId);
    long countByMediaId(String mediaId);

    @Query("SELECT AVG(r.score) FROM UserRatingEntity r WHERE r.mediaId = :mediaId")
    Double getAverageScoreByMediaId(@Param("mediaId") String mediaId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE UserRatingEntity r SET r.rankPosition = :rankPosition WHERE r.userId = :userId AND r.mediaId = :mediaId")
    void updateRankPosition(@Param("userId") UUID userId, @Param("mediaId") String mediaId, @Param("rankPosition") Integer rankPosition);

    @Query(value = """
        SELECT * FROM user_ratings r
        WHERE r.user_id = :userId
          AND (
            (:type = 'show' AND r.media_id LIKE 'tmdb-tv-%' AND r.media_id NOT LIKE '%-s%') OR
            (:type = 'season' AND r.media_id LIKE 'tmdb-tv-%' AND r.media_id LIKE '%-s%' AND r.media_id NOT LIKE '%-e%') OR
            (:type = 'episode' AND r.media_id LIKE 'tmdb-tv-%' AND r.media_id LIKE '%-e%') OR
            (:type = 'movie' AND r.media_id LIKE 'tmdb-movie-%') OR
            (:type = 'game' AND (r.media_id LIKE 'rawg-%' OR r.media_id LIKE 'igdb-%')) OR
            (:type = 'manga' AND (r.media_id LIKE 'manga-%' OR r.media_id LIKE 'mangadex-%' OR r.media_id LIKE 'anilist-%' OR r.media_id LIKE 'jikan-%')) OR
            (:type NOT IN ('show', 'season', 'episode', 'movie', 'game', 'manga'))
          )
        ORDER BY r.rank_position ASC NULLS LAST, r.score DESC
    """, countQuery = """
        SELECT count(*) FROM user_ratings r
        WHERE r.user_id = :userId
          AND (
            (:type = 'show' AND r.media_id LIKE 'tmdb-tv-%' AND r.media_id NOT LIKE '%-s%') OR
            (:type = 'season' AND r.media_id LIKE 'tmdb-tv-%' AND r.media_id LIKE '%-s%' AND r.media_id NOT LIKE '%-e%') OR
            (:type = 'episode' AND r.media_id LIKE 'tmdb-tv-%' AND r.media_id LIKE '%-e%') OR
            (:type = 'movie' AND r.media_id LIKE 'tmdb-movie-%') OR
            (:type = 'game' AND (r.media_id LIKE 'rawg-%' OR r.media_id LIKE 'igdb-%')) OR
            (:type = 'manga' AND (r.media_id LIKE 'manga-%' OR r.media_id LIKE 'mangadex-%' OR r.media_id LIKE 'anilist-%' OR r.media_id LIKE 'jikan-%')) OR
            (:type NOT IN ('show', 'season', 'episode', 'movie', 'game', 'manga'))
          )
    """, nativeQuery = true)
    Page<UserRatingEntity> findUserRankings(@Param("userId") UUID userId, @Param("type") String type, Pageable pageable);
}
