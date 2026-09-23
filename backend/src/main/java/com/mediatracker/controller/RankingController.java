package com.mediatracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/rankings")
@Tag(name = "Rankings", description = "Global media rankings and PageRank leaderboard")
public class RankingController {

    @PersistenceContext
    private EntityManager entityManager;

    private final com.mediatracker.repository.UserRatingRepository userRatingRepository;

    public RankingController(com.mediatracker.repository.UserRatingRepository userRatingRepository) {
        this.userRatingRepository = userRatingRepository;
    }

    public record RankingItem(String mediaId, Integer rankPosition) {}
    public record SaveRankingsRequest(List<RankingItem> rankings) {}

    @PostMapping
    @Operation(summary = "Save user ranking positions")
    public ResponseEntity<?> saveRankings(@RequestBody SaveRankingsRequest req) {
        UUID currentUserId = com.mediatracker.security.SecurityUtils.requireCurrentUserId();
        if (req.rankings() != null) {
            for (RankingItem item : req.rankings()) {
                if (item.mediaId() != null && item.rankPosition() != null) {
                    userRatingRepository.updateRankPosition(currentUserId, item.mediaId(), item.rankPosition());
                }
            }
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping
    @Operation(summary = "Get global ranked media filtered by media type and sorted by list rank, community score, or popularity")
    public ResponseEntity<?> getRankedMedia(
            @RequestParam(defaultValue = "SHOW") String type,
            @RequestParam(defaultValue = "list_rank") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        String mediaTypeUpper = type.trim().toUpperCase();

        String typeCondition = switch (mediaTypeUpper) {
            case "SEASON" -> "(s.media_type = 'SEASON' OR (s.media_type = 'SHOW' AND s.id LIKE '%-s%' AND s.id NOT LIKE '%-e%'))";
            case "EPISODE" -> "(s.media_type = 'EPISODE' OR (s.media_type = 'SHOW' AND s.id LIKE '%-e%'))";
            case "SHOW" -> "(s.media_type = 'SHOW' AND s.id NOT LIKE '%-s%' AND s.id NOT LIKE '%-e%')";
            case "MOVIE" -> "s.media_type = 'MOVIE'";
            case "GAME" -> "s.media_type = 'GAME'";
            case "MANGA" -> "s.media_type = 'MANGA'";
            default -> "s.media_type = '" + mediaTypeUpper + "'";
        };

        String countSql;
        if ("list_rank".equalsIgnoreCase(sort)) {
            countSql = "SELECT COUNT(DISTINCT s.id) FROM media_stats s INNER JOIN global_rankings g ON s.id = g.media_id WHERE " + typeCondition + " AND g.rank IS NOT NULL";
        } else {
            countSql = "SELECT COUNT(DISTINCT s.id) FROM media_stats s WHERE " + typeCondition;
        }

        Number countResult = (Number) entityManager.createNativeQuery(countSql).getSingleResult();
        long totalCount = countResult != null ? countResult.longValue() : 0L;

        String orderByClause = switch (sort.toLowerCase()) {
            case "community" -> "ORDER BY community_average DESC NULLS LAST, total_ratings DESC NULLS LAST";
            case "popular" -> "ORDER BY total_ratings DESC NULLS LAST, community_average DESC NULLS LAST";
            default -> "ORDER BY list_rank ASC NULLS LAST";
        };

        int offset = Math.max(0, (page - 1) * limit);

        String havingClause = "list_rank".equalsIgnoreCase(sort) ? "HAVING g.rank IS NOT NULL" : "";

        String dataSql = String.format("""
            WITH global_rankings_cte AS (
              SELECT 
                s.id AS media_id,
                s.media_type,
                s.community_average,
                s.total_ratings,
                MAX(r.media_title) AS title,
                MAX(r.media_image) AS image,
                MAX(r.media_release_date) AS release_date,
                g.elo_score AS average_rank,
                g.rank AS list_rank
              FROM media_stats s
              LEFT JOIN global_rankings g ON s.id = g.media_id
              LEFT JOIN user_ratings r ON s.id = r.media_id
              WHERE %s
              GROUP BY s.id, s.media_type, s.community_average, s.total_ratings, g.rank, g.elo_score
              %s
            )
            SELECT * FROM global_rankings_cte
            %s
            OFFSET %d LIMIT %d
        """, typeCondition, havingClause, orderByClause, offset, limit);

        Query q = entityManager.createNativeQuery(dataSql);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("media_id", row[0]);
            item.put("media_type", row[1]);
            item.put("community_average", row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
            item.put("total_ratings", row[3] != null ? ((Number) row[3]).longValue() : 0L);
            item.put("title", row[4] != null ? row[4] : row[0]);
            item.put("image", row[5]);
            item.put("releaseDate", row[6]);
            item.put("average_rank", row[7]);
            item.put("list_rank", row[8]);
            results.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "results", results,
                "count", totalCount
        ));
    }
}
