package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.dto.AnimeThemeDto;
import com.mediatracker.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class JikanClient {

    private static final Logger log = LoggerFactory.getLogger(JikanClient.class);
    private static final String BASE_URL = "https://api.jikan.moe/v4";
    private static final int CACHE_TTL_SECONDS = 7 * 24 * 3600; // 7 days

    private final RestClient restClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private final java.util.concurrent.atomic.AtomicLong lastRequestTime = new java.util.concurrent.atomic.AtomicLong(0);
    private static final long MIN_REQUEST_INTERVAL_MS = 350; // Max ~3 req/sec to respect MAL rate limits

    public JikanClient(RestClient restClient, CacheService cacheService, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    private void throttle() {
        long now = System.currentTimeMillis();
        long prev = lastRequestTime.get();
        long waitTime = (prev + MIN_REQUEST_INTERVAL_MS) - now;
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime.set(System.currentTimeMillis());
    }

    public Optional<AnimeThemeDto> getAnimeThemes(int malId) {
        String cacheKey = "jikan-themes-" + malId;
        Optional<AnimeThemeDto> cached = cacheService.get(cacheKey, AnimeThemeDto.class);
        if (cached.isPresent()) return cached;

        try {
            throttle();
            String uri = String.format("%s/anime/%d/themes", BASE_URL, malId);
            JsonNode json = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

            if (json == null || !json.has("data")) return Optional.empty();

            JsonNode data = json.get("data");
            List<String> openings = new ArrayList<>();
            List<String> endings = new ArrayList<>();

            if (data.has("openings") && data.get("openings").isArray()) {
                for (JsonNode op : data.get("openings")) {
                    openings.add(op.asText());
                }
            }
            if (data.has("endings") && data.get("endings").isArray()) {
                for (JsonNode ed : data.get("endings")) {
                    endings.add(ed.asText());
                }
            }

            AnimeThemeDto result = new AnimeThemeDto(openings, endings);
            cacheService.put(cacheKey, "jikan", result, CACHE_TTL_SECONDS);
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Failed to fetch Jikan themes for malId {}: {}", malId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<AnimeThemeDto> searchAnimeThemes(String query, String originalTitle, boolean isMovie) {
        if (query == null || query.isBlank()) return Optional.empty();
        String queryClean = query.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String cacheKey = (isMovie ? "jikan-themes-movie-" : "jikan-themes-") + queryClean;

        Optional<AnimeThemeDto> cached = cacheService.get(cacheKey, AnimeThemeDto.class);
        if (cached.isPresent()) return cached;

        List<String> searchQueries = new ArrayList<>();
        searchQueries.add(query.trim());
        if (originalTitle != null && !originalTitle.trim().equalsIgnoreCase(query.trim())) {
            searchQueries.add(originalTitle.trim());
        }

        String typeFilter = isMovie ? "&type=movie" : "";

        for (String q : searchQueries) {
            try {
                String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
                String uri = String.format("%s/anime?q=%s%s&limit=1", BASE_URL, encoded, typeFilter);

                throttle();
                JsonNode json = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(JsonNode.class);

                if (json == null || !json.has("data") || !json.get("data").isArray() || json.get("data").isEmpty()) {
                    continue;
                }

                JsonNode item = json.get("data").get(0);
                List<String> openings = new ArrayList<>();
                List<String> endings = new ArrayList<>();

                if (item.has("theme")) {
                    JsonNode themeNode = item.get("theme");
                    if (themeNode.has("openings") && themeNode.get("openings").isArray()) {
                        for (JsonNode op : themeNode.get("openings")) openings.add(op.asText());
                    }
                    if (themeNode.has("endings") && themeNode.get("endings").isArray()) {
                        for (JsonNode ed : themeNode.get("endings")) endings.add(ed.asText());
                    }
                } else if (item.has("mal_id")) {
                    int malId = item.get("mal_id").asInt();
                    Optional<AnimeThemeDto> themes = getAnimeThemes(malId);
                    if (themes.isPresent()) {
                        openings = themes.get().getOpenings();
                        endings = themes.get().getEndings();
                    }
                }

                if (!openings.isEmpty() || !endings.isEmpty()) {
                    AnimeThemeDto result = new AnimeThemeDto(openings, endings);
                    cacheService.put(cacheKey, "jikan", result, CACHE_TTL_SECONDS);
                    return Optional.of(result);
                }
            } catch (Exception e) {
                log.warn("Jikan search error for '{}': {}", q, e.getMessage());
            }
        }

        return Optional.empty();
    }
}
