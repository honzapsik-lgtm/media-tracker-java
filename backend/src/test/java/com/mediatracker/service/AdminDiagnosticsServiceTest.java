package com.mediatracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class AdminDiagnosticsServiceTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static JdbcTemplate jdbc;
    AdminDiagnosticsService service;

    @BeforeAll
    static void schema() {
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__init_schema.sql")).execute(source);
        jdbc = new JdbcTemplate(source);
    }

    @BeforeEach
    void setup() {
        jdbc.execute("TRUNCATE users, user_ratings, user_watchlist, media, media_stats, \"UserStatsCache\", \"Account\", \"Session\", \"ApiCache\", \"SystemLog\", \"BackgroundJob\" CASCADE");
        service = new AdminDiagnosticsService(jdbc, new ObjectMapper());
    }

    @Test
    void cacheFiltersPagingAndSummaryUseActualRowsWithoutPayloads() {
        jdbc.update("INSERT INTO \"ApiCache\" (id, provider, data, expires_at) VALUES ('discover-movies', 'tmdb', '{\"secret\":true}', CURRENT_TIMESTAMP - INTERVAL '1 hour'), ('tmdb-search-a', 'tmdb', '{}', CURRENT_TIMESTAMP + INTERVAL '1 hour'), ('other', 'igdb', '{}', CURRENT_TIMESTAMP + INTERVAL '1 hour')");
        var summary = service.cacheSummary();
        assertEquals(3L, summary.get("totalEntries"));
        assertEquals(1L, summary.get("expiredEntries"));
        assertEquals(Map.of("igdb", 1L, "tmdb", 2L), summary.get("byProvider"));
        assertEquals(Map.of("discover", 1L, "search", 1L, "detail", 1L), summary.get("byType"));
        assertTrue((Long) summary.get("oldestExpiredAgeSeconds") >= 3600);
        var page = service.cache(Map.of("provider", "tmdb", "expired", "false", "type", "search"));
        var items = items(page);
        assertEquals(1, items.size());
        assertEquals("tmdb-search-a", items.getFirst().get("id"));
        assertFalse(items.getFirst().containsKey("data"));
        assertEquals(2, ((Number) items.getFirst().get("payloadSizeBytes")).intValue());
        assertEquals(1, items(service.cache(Map.of("page", "2", "pageSize", "1", "sort", "expires_asc"))).size());
        assertTrue(items(service.cache(Map.of("q", "' OR 1=1 --"))).isEmpty());
        var pagination = (Map<?, ?>) service.cache(Map.of("page", "-1", "pageSize", "999", "sort", "id; DROP TABLE users")).get("pagination");
        assertEquals(1, pagination.get("page"));
        assertEquals(100, pagination.get("pageSize"));
    }

    @Test
    void logsAndPerformanceHaveMatchingFilteredTotalsAndParsedMetadata() {
        jdbc.update("INSERT INTO \"SystemLog\" (id, level, event, message, \"durationMs\", metadata, \"userId\", \"requestId\") VALUES ('a', 'warn', 'performance.slow_operation', 'slow', 500, '{\"operation\":\"search\"}', 'user', 'request'), ('b', 'error', 'performance.slow_operation', 'slower', 1500, '{\"operation\":\"detail\"}', 'other', 'request')");
        jdbc.update("INSERT INTO \"SystemLog\" (id, level, event, \"createdAt\") VALUES ('old', 'error', 'performance.slow_operation', CURRENT_TIMESTAMP - INTERVAL '25 hours')");
        var summary = service.performanceSummary();
        assertEquals(2L, summary.get("last24Hours"));
        assertEquals("b", ((Map<?, ?>) summary.get("slowestLast24Hours")).get("id"));
        var result = service.logs(Map.of("operation", "search", "userId", "user", "requestId", "request", "sinceHours", "24"), true);
        assertEquals(1L, ((Map<?, ?>) result.get("pagination")).get("total"));
        assertEquals(Map.of("operation", "search"), items(result).getFirst().get("metadata"));
        assertTrue(items(service.logs(Map.of("q", "unmatched"), false)).isEmpty());
        assertEquals(1L, service.logSummary().get("errorsLastHour"));
        assertEquals(1L, service.logSummary().get("warningsLast24Hours"));
    }

    @Test
    void integrityFindsMissingAggregatesAndUsersWithoutCache() {
        jdbc.update("INSERT INTO users (id, username) VALUES ('00000000-0000-0000-0000-000000000001', 'test')");
        jdbc.update("INSERT INTO user_ratings (user_id, media_id, score, review_text, is_deep_review) VALUES ('00000000-0000-0000-0000-000000000001', 'movie', 101, 'review', true)");
        jdbc.update("INSERT INTO media_stats (id, total_ratings, media_type) VALUES ('stale', 2, 'MOVIE')");
        var checks = service.integrityChecks();
        assertEquals(1L, checks.get(0).get("count"));
        assertEquals("error", checks.get(0).get("severity"));
        assertEquals(2L, checks.get(1).get("count"));
        assertEquals(1L, checks.get(2).get("count"));
        var summary = service.databaseSummary();
        assertEquals(1L, summary.get("ratingsWithReviewText"));
        assertEquals(1L, summary.get("deepReviews"));
        assertEquals(1L, summary.get("users"));
        jdbc.update("UPDATE user_ratings SET score = 50");
        jdbc.update("UPDATE media_stats SET total_ratings = 0");
        jdbc.update("INSERT INTO media_stats (id, total_ratings, media_type) VALUES ('movie', 1, 'MOVIE')");
        jdbc.update("INSERT INTO \"UserStatsCache\" (user_id, stats_json, updated_at, media_type) VALUES ('00000000-0000-0000-0000-000000000001', '{}', CURRENT_TIMESTAMP, 'MOVIE')");
        assertTrue(service.integrityChecks().stream().allMatch(check -> check.get("severity").equals("ok")));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("items");
    }

    @Test
    void userDiagnosticsReadRelatedRowsWithoutCredentialsOrUnrelatedUsers() throws Exception {
        String id = "00000000-0000-0000-0000-000000000001";
        String other = "00000000-0000-0000-0000-000000000002";
        jdbc.update("INSERT INTO users (id, username, email) VALUES (?::uuid, 'Alice', 'alice@example.test'), (?::uuid, 'Bob', 'bob@example.test')", id, other);
        jdbc.update("INSERT INTO \"Account\" (id, \"userId\", type, provider, \"providerAccountId\", access_token, refresh_token, id_token) VALUES ('account', ?::uuid, 'oauth', 'google', 'external', 'ACCESS_SECRET', 'REFRESH_SECRET', 'ID_SECRET')", id);
        jdbc.update("INSERT INTO \"Session\" (id, \"userId\", \"sessionToken\", expires) VALUES ('session', ?::uuid, 'SESSION_SECRET', CURRENT_TIMESTAMP + INTERVAL '1 day')", id);
        jdbc.update("INSERT INTO \"UserStatsCache\" (user_id, media_type, stats_json, updated_at) VALUES (?::uuid, 'MOVIE', '{\"rated\":3}', CURRENT_TIMESTAMP)", id);
        jdbc.update("INSERT INTO user_ratings (user_id, media_id, score) VALUES (?::uuid, 'tmdb-movie-1', 80)", id);
        jdbc.update("INSERT INTO user_watchlist (user_id, media_id) VALUES (?::uuid, 'tmdb-movie-1')", id);
        jdbc.update("INSERT INTO user_badges (user_id, badge_id) VALUES (?::uuid, 'badge')", id);
        jdbc.update("INSERT INTO user_lists (user_id, title, media_type) VALUES (?::uuid, 'list', 'MOVIE')", id);
        jdbc.update("INSERT INTO \"BackgroundJob\" (id, type, payload, updated_at) VALUES ('mine', 'update_user_stats', jsonb_build_object('userId', ?::text), CURRENT_TIMESTAMP), ('other', 'update_user_stats', jsonb_build_object('userId', ?::text), CURRENT_TIMESTAMP)", id, other);
        jdbc.update("INSERT INTO \"SystemLog\" (id, level, event, \"userId\") VALUES ('mine', 'info', 'test', ?), ('other', 'info', 'test', ?)", id, other);
        var result = service.userDiagnostics("aLiCe").orElseThrow();
        assertEquals(id, ((Map<?, ?>) result.get("user")).get("id").toString());
        assertEquals(Map.of("ratings", 1L, "watchlist", 1L, "badges", 1L, "rankedLists", 1L), result.get("aggregates"));
        assertEquals(Set.of("id", "userId", "type", "provider", "providerAccountId"), rows(result, "accounts").getFirst().keySet());
        assertEquals(Set.of("id", "userId", "expires"), rows(result, "sessions").getFirst().keySet());
        assertEquals(Map.of("rated", 3), rows(result, "statsCache").getFirst().get("stats_json"));
        assertEquals("MOVIE", rows(result, "statsCache").getFirst().get("media_type"));
        assertEquals(List.of("mine"), rows(result, "jobs").stream().map(row -> row.get("id")).toList());
        assertNotNull(rows(result, "jobs").getFirst().get("created_at"));
        assertEquals(List.of("mine"), rows(result, "logs").stream().map(row -> row.get("id")).toList());
        assertFalse(new ObjectMapper().findAndRegisterModules().writeValueAsString(result).contains("SECRET"));
        assertTrue(service.userDiagnostics(id).isPresent());
        assertTrue(service.userDiagnostics("alice@example.test").isPresent());
        assertTrue(service.userDiagnostics("missing").isEmpty());
        assertTrue(service.userDiagnostics("' OR 1=1 --").isEmpty());
        var bob = service.userDiagnostics(other).orElseThrow();
        assertTrue(rows(bob, "accounts").isEmpty());
        assertTrue(rows(bob, "sessions").isEmpty());
        assertTrue(rows(bob, "statsCache").isEmpty());
    }

    @Test
    void mediaDiagnosticsUseExactEngagementCountsAndProviderCacheKeys() {
        String mediaId = "tmdb-tv-12";
        jdbc.update("INSERT INTO users (id) VALUES ('00000000-0000-0000-0000-000000000001'), ('00000000-0000-0000-0000-000000000002')");
        jdbc.update("INSERT INTO media (id, type) VALUES (?, 'SHOW')", mediaId);
        jdbc.update("INSERT INTO media_stats (id, media_type, total_ratings, community_average) VALUES (?, 'SHOW', 9, 75)", mediaId);
        jdbc.update("INSERT INTO user_ratings (user_id, media_id, score, review_text, is_deep_review) VALUES ('00000000-0000-0000-0000-000000000001', ?, 80, 'Review', true), ('00000000-0000-0000-0000-000000000002', ?, 70, '   ', false), ('00000000-0000-0000-0000-000000000001', 'tmdb-tv-123', 90, 'Other', true)", mediaId, mediaId);
        jdbc.update("INSERT INTO user_watchlist (user_id, media_id) VALUES ('00000000-0000-0000-0000-000000000001', ?), ('00000000-0000-0000-0000-000000000002', ?), ('00000000-0000-0000-0000-000000000001', 'tmdb-tv-123')", mediaId, mediaId);
        for (String key : List.of(mediaId, "tmdb-details-v2-tv-12", "tmdb-episodes-12-1", "tmdb-tv-123", "tmdb-episodes-123-1", "tmdb-details-v2-movie-12")) {
            jdbc.update("INSERT INTO \"ApiCache\" (id, provider, data, expires_at) VALUES (?, 'tmdb', '{\"title\":\"Test\"}', CURRENT_TIMESTAMP - INTERVAL '1 hour')", key);
        }
        jdbc.update("INSERT INTO \"ApiCache\" (id, provider, data, expires_at) VALUES ('igdb-access-token', 'igdb', '\"SECRET\"', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO \"SystemLog\" (id, level, event, \"mediaId\") VALUES ('mine', 'info', 'test', ?), ('other', 'info', 'test', 'tmdb-tv-123')", mediaId);
        var result = service.mediaDiagnostics(mediaId);
        assertEquals(Map.of("id", mediaId, "type", "SHOW"), result.get("tracking"));
        assertEquals(Map.of("totalRatings", 2L, "writtenReviews", 1L, "deepReviews", 1L, "watchlistInclusions", 2L), result.get("aggregations"));
        assertEquals(9, ((Map<?, ?>) result.get("stats")).get("total_ratings"));
        assertEquals(Set.of(mediaId, "tmdb-details-v2-tv-12", "tmdb-episodes-12-1"), new HashSet<>(rows(result, "caches").stream().map(row -> row.get("id")).toList()));
        assertEquals(Map.of("title", "Test"), rows(result, "caches").getFirst().get("data"));
        assertNotNull(rows(result, "caches").getFirst().get("expires_at"));
        assertEquals(List.of("mine"), rows(result, "logs").stream().map(row -> row.get("id")).toList());
        assertTrue(rows(service.mediaDiagnostics("igdb-access-token"), "caches").isEmpty());
        var missing = service.mediaDiagnostics("tmdb-tv-999");
        assertNull(missing.get("stats"));
        assertEquals(0L, ((Map<?, ?>) missing.get("aggregations")).get("watchlistInclusions"));
        assertTrue(rows(missing, "caches").isEmpty());
        assertTrue(rows(missing, "logs").isEmpty());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> result, String key) {
        return (List<Map<String, Object>>) result.get(key);
    }

    @Test
    void recentUserDiagnosticsAreBoundedAndNewestFirst() {
        String id = "00000000-0000-0000-0000-000000000001";
        jdbc.update("INSERT INTO users (id) VALUES (?::uuid)", id);
        jdbc.update("INSERT INTO \"BackgroundJob\" (id, type, payload, created_at, updated_at) SELECT n::text, 'update_user_stats', jsonb_build_object('userId', ?::text), CURRENT_TIMESTAMP - n * INTERVAL '1 minute', CURRENT_TIMESTAMP FROM generate_series(1, 55) n", id);
        jdbc.update("INSERT INTO \"SystemLog\" (id, level, event, \"userId\", \"createdAt\", \"errorStack\") SELECT n::text, 'error', 'test', ?, CURRENT_TIMESTAMP - n * INTERVAL '1 minute', 'stack' FROM generate_series(1, 55) n", id);
        var result = service.userDiagnostics(id).orElseThrow();
        assertEquals(50, rows(result, "jobs").size());
        assertEquals(50, rows(result, "logs").size());
        assertEquals("1", rows(result, "jobs").getFirst().get("id"));
        assertEquals("1", rows(result, "logs").getFirst().get("id"));
        assertEquals("stack", rows(result, "logs").getFirst().get("errorStack"));
    }
}
