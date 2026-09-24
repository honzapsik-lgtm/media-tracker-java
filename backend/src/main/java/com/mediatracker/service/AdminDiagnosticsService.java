package com.mediatracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminDiagnosticsService {
    private static final String CACHE_TYPE = "CASE WHEN id LIKE 'discover-%' THEN 'discover' WHEN id LIKE '%-search-%' THEN 'search' WHEN id LIKE '%trending%' THEN 'trending' ELSE 'detail' END";
    private static final String OPERATION = "COALESCE(metadata->>'operation', 'unknown')";
    private static final String SLOW = "event = 'performance.slow_operation'";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public AdminDiagnosticsService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    private long count(String sql, Object... args) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class, args));
    }

    private Map<String, Long> groups(String table, String expression) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query("SELECT " + expression + " AS name, COUNT(*) AS count FROM " + table + " GROUP BY 1 ORDER BY 1",
                rs -> { result.put(rs.getString("name"), rs.getLong("count")); });
        return result;
    }

    public Map<String, Object> databaseSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> tables = Map.ofEntries(
                Map.entry("users", "users"), Map.entry("accounts", "\"Account\""),
                Map.entry("sessions", "\"Session\""), Map.entry("ratings", "user_ratings"),
                Map.entry("watchlistEntries", "user_watchlist"), Map.entry("mediaStats", "media_stats"),
                Map.entry("userBadges", "user_badges"), Map.entry("userStatsCache", "\"UserStatsCache\""),
                Map.entry("apiCache", "\"ApiCache\""));
        tables.forEach((name, table) -> result.put(name, count("SELECT COUNT(*) FROM " + table)));
        result.put("ratingsWithReviewText", count("SELECT COUNT(*) FROM user_ratings WHERE NULLIF(BTRIM(review_text), '') IS NOT NULL"));
        result.put("deepReviews", count("SELECT COUNT(*) FROM user_ratings WHERE is_deep_review = true"));
        result.put("backgroundJobsByStatus", groups("\"BackgroundJob\"", "status"));
        result.put("systemLogsByLevel", groups("\"SystemLog\"", "level"));
        return result;
    }

    public Optional<Map<String, Object>> userDiagnostics(String identifier) {
        List<Map<String, Object>> users;
        String projection = "SELECT id, email, name, username, role, created_at, image FROM users WHERE ";
        try {
            users = diagnosticRows(projection + "id = ?", UUID.fromString(identifier));
        } catch (IllegalArgumentException e) {
            users = diagnosticRows(projection + "LOWER(username) = LOWER(?) OR email = ? ORDER BY CASE WHEN LOWER(username) = LOWER(?) THEN 0 ELSE 1 END, id LIMIT 1",
                    identifier, identifier, identifier);
        }
        if (users.isEmpty()) return Optional.empty();
        var user = users.getFirst();
        UUID userId = UUID.fromString(user.get("id").toString());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        // Explicit allowlists keep OAuth credentials and session tokens out of diagnostics.
        result.put("accounts", diagnosticRows("SELECT id, \"userId\", type, provider, \"providerAccountId\" FROM \"Account\" WHERE \"userId\" = ? ORDER BY provider, id", userId));
        result.put("sessions", diagnosticRows("SELECT id, \"userId\", expires FROM \"Session\" WHERE \"userId\" = ? ORDER BY expires DESC, id", userId));
        result.put("statsCache", diagnosticRows("SELECT user_id, media_type, stats_json, updated_at FROM \"UserStatsCache\" WHERE user_id = ? ORDER BY media_type", userId));
        result.put("aggregates", Map.of(
                "ratings", count("SELECT COUNT(*) FROM user_ratings WHERE user_id = ?", userId),
                "watchlist", count("SELECT COUNT(*) FROM user_watchlist WHERE user_id = ?", userId),
                "badges", count("SELECT COUNT(*) FROM user_badges WHERE user_id = ?", userId),
                "rankedLists", count("SELECT COUNT(*) FROM user_lists WHERE user_id = ?", userId)));
        result.put("jobs", diagnosticRows("SELECT id, type, status, attempts, max_attempts, run_at, locked_at, last_error, created_at, updated_at, processed_at FROM \"BackgroundJob\" WHERE payload->>'userId' = ? ORDER BY created_at DESC, id DESC LIMIT 50", userId.toString()));
        result.put("logs", logRows("WHERE \"userId\" = ? ORDER BY \"createdAt\" DESC, id DESC LIMIT 50", List.of(userId.toString())));
        return Optional.of(result);
    }

    public Map<String, Object> mediaDiagnostics(String mediaId) {
        var media = diagnosticRows("SELECT id, type::text AS type FROM media WHERE id = ?", mediaId);
        var stats = diagnosticRows("SELECT id, community_average, total_ratings, media_type::text AS media_type FROM media_stats WHERE id = ?", mediaId);
        Map<String, Object> tracking = new LinkedHashMap<>();
        tracking.put("id", mediaId);
        tracking.put("type", !media.isEmpty() ? media.getFirst().get("type")
                : !stats.isEmpty() ? stats.getFirst().get("media_type") : null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tracking", tracking);
        result.put("stats", stats.isEmpty() ? null : stats.getFirst());
        result.put("aggregations", Map.of(
                "totalRatings", count("SELECT COUNT(*) FROM user_ratings WHERE media_id = ?", mediaId),
                "writtenReviews", count("SELECT COUNT(*) FROM user_ratings WHERE media_id = ? AND NULLIF(BTRIM(review_text), '') IS NOT NULL", mediaId),
                "deepReviews", count("SELECT COUNT(*) FROM user_ratings WHERE media_id = ? AND is_deep_review = true", mediaId),
                "watchlistInclusions", count("SELECT COUNT(*) FROM user_watchlist WHERE media_id = ?", mediaId)));
        result.put("caches", mediaCaches(mediaId));
        result.put("logs", logRows("WHERE \"mediaId\" = ? ORDER BY \"createdAt\" DESC, id DESC LIMIT 50", List.of(mediaId)));
        return result;
    }

    private List<Map<String, Object>> mediaCaches(String mediaId) {
        // Only media-shaped keys may expose payloads; ApiCache also stores provider credentials.
        if (!mediaId.matches("(tmdb-(movie|tv)|anilist-(manga|anime)|igdb-game|rawg-game)-[0-9]+")
                && !mediaId.matches("mangadex-manga-[0-9a-fA-F-]{36}")) return List.of();
        List<String> keys = new ArrayList<>(List.of(mediaId));
        List<String> prefixes = new ArrayList<>(List.of(mediaId + "-"));
        String[] parts = mediaId.split("-", 3);
        if (parts[0].equals("tmdb")) {
            keys.add("tmdb-details-v2-" + parts[1] + "-" + parts[2]);
            keys.add("tmdb-rec-" + parts[1] + "-" + parts[2]);
            if (parts[1].equals("tv")) {
                prefixes.add("tmdb-episodes-" + parts[2] + "-");
                prefixes.add("tmdb-episode-credits-" + parts[2] + "-");
            }
        } else if (parts[0].equals("anilist")) {
            keys.add("anilist-" + parts[2]);
            keys.add("anilist-dto-" + parts[2]);
        } else if (parts[0].equals("mangadex")) {
            keys.add("mangadex-details-v2-" + parts[2]);
            prefixes.add("mangadex-chapters-" + parts[2] + "-");
        }
        List<String> predicates = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        keys.forEach(key -> { predicates.add("id = ?"); args.add(key); });
        prefixes.forEach(prefix -> { predicates.add("starts_with(id, ?)"); args.add(prefix); });
        args.add(mediaId);
        return diagnosticRows("SELECT id, provider, data, created_at, expires_at FROM \"ApiCache\" WHERE "
                + String.join(" OR ", predicates) + " ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END, created_at DESC, id LIMIT 100",
                args.toArray());
    }

    private List<Map<String, Object>> diagnosticRows(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> {
            Map<String, Object> result = new LinkedHashMap<>();
            var columns = rs.getMetaData();
            for (int i = 1; i <= columns.getColumnCount(); i++) {
                Object value = rs.getObject(i);
                if (value instanceof org.postgresql.util.PGobject pg) {
                    try {
                        value = pg.getType().equals("jsonb") || pg.getType().equals("json")
                                ? mapper.readValue(pg.getValue(), Object.class) : pg.getValue();
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Invalid diagnostics JSON", e);
                    }
                } else if (value instanceof Timestamp timestamp) {
                    value = timestamp.toLocalDateTime();
                }
                result.put(columns.getColumnLabel(i), value);
            }
            return result;
        }, args);
    }

    public List<Map<String, Object>> integrityChecks() {
        long invalid = count("SELECT COUNT(*) FROM user_ratings WHERE score < 0 OR score > 100");
        // Full join also catches ratings whose aggregate row is entirely missing.
        long mismatches = count("""
                SELECT COUNT(*) FROM media_stats s FULL JOIN
                (SELECT media_id, COUNT(*) AS n FROM user_ratings GROUP BY media_id) r ON r.media_id = s.id
                WHERE s.id IS NULL OR COALESCE(s.total_ratings, 0) <> COALESCE(r.n, 0)
                """);
        long missing = count("SELECT COUNT(*) FROM users u WHERE NOT EXISTS (SELECT 1 FROM \"UserStatsCache\" c WHERE c.user_id = u.id)");
        return List.of(
                check("ratings_out_of_range", "User ratings valid (0-100)", invalid, "error", "Ratings outside the valid 0-100 range."),
                check("media_stats_inconsistencies", "MediaStats matches user ratings", mismatches, "warning", "Missing or inconsistent MediaStats rating counts."),
                check("users_missing_stats_cache", "Users have stats cache", missing, "warning", "Users without any UserStatsCache rows."));
    }

    private Map<String, Object> check(String id, String label, long count, String severity, String detail) {
        return Map.of("id", id, "label", label, "count", count, "severity", count == 0 ? "ok" : severity,
                "detail", count == 0 ? "No issues found." : detail);
    }

    public Map<String, Object> logSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String level : List.of("error", "warn")) {
            String key = level.equals("error") ? "errors" : "warnings";
            result.put(key + "LastHour", count("SELECT COUNT(*) FROM \"SystemLog\" WHERE level = ? AND \"createdAt\" >= CURRENT_TIMESTAMP - INTERVAL '1 hour'", level));
            result.put(key + "Last24Hours", count("SELECT COUNT(*) FROM \"SystemLog\" WHERE level = ? AND \"createdAt\" >= CURRENT_TIMESTAMP - INTERVAL '24 hours'", level));
        }
        result.put("latestErrorLogs", logRows("WHERE level = ? ORDER BY \"createdAt\" DESC, id DESC LIMIT 5", List.of("error")));
        return result;
    }

    public Map<String, Object> performanceSummary() {
        String recent = " FROM \"SystemLog\" WHERE " + SLOW + " AND \"createdAt\" >= CURRENT_TIMESTAMP - INTERVAL '24 hours'";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lastHour", count("SELECT COUNT(*) FROM \"SystemLog\" WHERE " + SLOW + " AND \"createdAt\" >= CURRENT_TIMESTAMP - INTERVAL '1 hour'"));
        result.put("last24Hours", count("SELECT COUNT(*)" + recent));
        var slowest = logRows("WHERE " + SLOW + " AND \"createdAt\" >= CURRENT_TIMESTAMP - INTERVAL '24 hours' ORDER BY \"durationMs\" DESC NULLS LAST, \"createdAt\" DESC, id DESC LIMIT 1", List.of());
        result.put("slowestLast24Hours", slowest.isEmpty() ? null : slowest.getFirst());
        result.put("topOperationsLast24Hours", jdbc.queryForList("SELECT " + OPERATION + " AS operation, COUNT(*) AS count" + recent + " GROUP BY 1 ORDER BY count DESC, operation LIMIT 10"));
        return result;
    }

    public Map<String, Object> logs(Map<String, String> filters, boolean performance) {
        StringBuilder where = new StringBuilder(performance ? "WHERE " + SLOW : "WHERE 1=1");
        List<Object> args = new ArrayList<>();
        for (String field : List.of("level", "requestId", "userId", "mediaId", "jobId")) {
            filter(where, args, "\"" + field + "\" = ?", filters.get(field));
        }
        filter(where, args, "POSITION(? IN event) > 0", filters.get("event"));
        filter(where, args, "POSITION(? IN " + OPERATION + ") > 0", filters.get("operation"));
        filter(where, args, "POSITION(LOWER(?) IN LOWER(CONCAT_WS(' ', event, message, \"errorName\", \"errorMessage\"))) > 0", filters.get("q"));
        if (filters.containsKey("sinceHours")) {
            where.append(" AND \"createdAt\" >= CURRENT_TIMESTAMP - (? * INTERVAL '1 hour')");
            args.add(positive(filters.get("sinceHours"), 24));
        }
        long total = count("SELECT COUNT(*) FROM \"SystemLog\" " + where, args.toArray());
        int page = positive(filters.get("page"), 1), size = Math.min(100, positive(filters.get("pageSize"), 50));
        args.add(size);
        args.add((long) (page - 1) * size);
        return paged(logRows(where + " ORDER BY \"createdAt\" DESC, id DESC LIMIT ? OFFSET ?", args), page, size, total);
    }

    private List<Map<String, Object>> logRows(String suffix, List<Object> args) {
        return jdbc.query("SELECT *, " + OPERATION + " AS operation FROM \"SystemLog\" " + suffix, (rs, row) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            for (String field : List.of("id", "level", "event", "message", "requestId", "userId", "mediaId", "mediaType", "jobId", "errorName", "errorMessage", "errorStack", "operation")) item.put(field, rs.getString(field));
            item.put("durationMs", rs.getObject("durationMs"));
            item.put("createdAt", rs.getTimestamp("createdAt").toLocalDateTime());
            String metadata = rs.getString("metadata");
            try { item.put("metadata", metadata == null ? null : mapper.readValue(metadata, Object.class)); }
            catch (JsonProcessingException e) { throw new IllegalStateException("Invalid SystemLog metadata", e); }
            return item;
        }, args.toArray());
    }

    public Map<String, Object> cacheSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = count("SELECT COUNT(*) FROM \"ApiCache\"");
        long expired = count("SELECT COUNT(*) FROM \"ApiCache\" WHERE expires_at <= CURRENT_TIMESTAMP");
        result.put("totalEntries", total);
        result.put("expiredEntries", expired);
        result.put("freshEntries", total - expired);
        result.put("oldestExpiredAgeSeconds", jdbc.queryForObject("SELECT EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - MIN(expires_at)))::bigint FROM \"ApiCache\" WHERE expires_at <= CURRENT_TIMESTAMP", Long.class));
        result.put("byProvider", groups("\"ApiCache\"", "provider"));
        result.put("byType", groups("\"ApiCache\"", CACHE_TYPE));
        var cleanup = logRows("WHERE event IN ('admin.cache.cleanup.completed', 'admin.cache.cleanup', 'cache.cleanup.completed') ORDER BY \"createdAt\" DESC, id DESC LIMIT 1", List.of());
        result.put("lastCleanupLog", cleanup.isEmpty() ? null : cleanup.getFirst());
        return result;
    }

    public Map<String, Object> cache(Map<String, String> filters) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> args = new ArrayList<>();
        filter(where, args, "POSITION(LOWER(?) IN LOWER(id)) > 0", filters.get("q"));
        filter(where, args, "provider = ?", filters.get("provider"));
        filter(where, args, "(" + CACHE_TYPE + ") = ?", filters.get("type"));
        if ("true".equals(filters.get("expired"))) where.append(" AND expires_at <= CURRENT_TIMESTAMP");
        if ("false".equals(filters.get("expired"))) where.append(" AND expires_at > CURRENT_TIMESTAMP");
        long total = count("SELECT COUNT(*) FROM \"ApiCache\" " + where, args.toArray());
        String order = switch (filters.getOrDefault("sort", "created_desc")) {
            case "created_asc" -> "created_at ASC";
            case "expires_asc" -> "expires_at ASC";
            case "expires_desc" -> "expires_at DESC";
            default -> "created_at DESC";
        };
        int page = positive(filters.get("page"), 1), size = Math.min(100, positive(filters.get("pageSize"), 50));
        args.add(size);
        args.add((long) (page - 1) * size);
        var rows = jdbc.queryForList("SELECT id, id AS key, provider, created_at AS \"createdAt\", created_at AS \"updatedAt\", expires_at AS \"expiresAt\", expires_at <= CURRENT_TIMESTAMP AS expired, octet_length(data::text) AS \"payloadSizeBytes\", " + CACHE_TYPE + " AS type FROM \"ApiCache\" " + where + " ORDER BY " + order + ", id ASC LIMIT ? OFFSET ?", args.toArray());
        rows.forEach(row -> row.replaceAll((key, value) -> value instanceof Timestamp t ? t.toLocalDateTime() : value));
        return paged(rows, page, size, total);
    }

    private void filter(StringBuilder where, List<Object> args, String predicate, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(predicate);
            args.add(value.trim());
        }
    }

    private int positive(String value, int fallback) {
        try { return Math.max(1, Integer.parseInt(value)); }
        catch (NumberFormatException e) { return fallback; }
    }

    private Map<String, Object> paged(List<Map<String, Object>> items, int page, int size, long total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("pagination", Map.of("page", page, "pageSize", size, "total", total, "pageCount", Math.max(1, (total + size - 1) / size)));
        return result;
    }
}
