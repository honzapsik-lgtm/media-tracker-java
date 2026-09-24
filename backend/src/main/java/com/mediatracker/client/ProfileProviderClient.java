package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mediatracker.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

@Component
public class ProfileProviderClient {
    private static final Logger log = LoggerFactory.getLogger(ProfileProviderClient.class);
    private final RestClient restClient;
    private final CacheService cache;
    private final IgdbClient igdb;
    private final String tmdbKey;
    private final String rawgKey;
    private final String twitchClientId;

    public ProfileProviderClient(RestClient restClient, CacheService cache, IgdbClient igdb,
            @Value("${app.providers.tmdb.api-key:}") String tmdbKey,
            @Value("${app.providers.rawg.api-key:}") String rawgKey,
            @Value("${app.providers.twitch.client-id:}") String twitchClientId) {
        this.restClient = restClient;
        this.cache = cache;
        this.igdb = igdb;
        this.tmdbKey = tmdbKey;
        this.rawgKey = rawgKey;
        this.twitchClientId = twitchClientId;
    }

    public Optional<JsonNode> tmdb(String path, Map<String, ?> params) {
        if (tmdbKey.isBlank()) return Optional.empty();
        return cached("tmdb", path + new TreeMap<>(params), () -> get("https://api.themoviedb.org/3" + path, params, "api_key", tmdbKey));
    }

    public Optional<JsonNode> rawg(String path, Map<String, ?> params) {
        if (rawgKey.isBlank()) return Optional.empty();
        return cached("rawg", path + new TreeMap<>(params), () -> get("https://api.rawg.io/api" + path, params, "key", rawgKey));
    }

    public Optional<JsonNode> mangadex(String path, Map<String, ?> params) {
        return cached("mangadex", path + new TreeMap<>(params), () -> get("https://api.mangadex.org" + path, params, null, null));
    }

    public Optional<JsonNode> igdb(String endpoint, int id, String fields) {
        return cached("igdb", endpoint + "-" + id, () -> {
            String token = igdb.getAccessToken();
            if (token == null || token.isBlank() || twitchClientId.isBlank()) return null;
            JsonNode result = restClient.post().uri("https://api.igdb.com/v4/" + endpoint)
                    .header("Client-ID", twitchClientId).header("Authorization", "Bearer " + token)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("fields " + fields + "; where id = " + id + ";")
                    .retrieve().body(JsonNode.class);
            return result != null && result.isArray() && !result.isEmpty() ? result.get(0) : null;
        });
    }

    public Optional<JsonNode> anilist(String key, String query, Map<String, ?> variables, boolean retry) {
        return cached("anilist", key, () -> {
            for (int attempt = 0; attempt < (retry ? 3 : 1); attempt++) {
                try {
                    JsonNode result = restClient.post().uri("https://graphql.anilist.co")
                            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                            .body(Map.of("query", query, "variables", variables)).retrieve().body(JsonNode.class);
                    return result == null || result.hasNonNull("errors") ? null : result.get("data");
                } catch (RestClientResponseException e) {
                    if (e.getStatusCode().value() != 429 || !retry || attempt == 2) throw e;
                    long seconds = 5;
                    try {
                        if (e.getResponseHeaders() != null) seconds = Long.parseLong(e.getResponseHeaders().getFirst("Retry-After"));
                    } catch (NumberFormatException ignored) { }
                    try {
                        Thread.sleep(Math.max(0, Math.min(seconds, 60)) * 1000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
            return null;
        });
    }

    private JsonNode get(String url, Map<String, ?> params, String keyName, String key) {
        var uri = UriComponentsBuilder.fromUriString(url);
        if (keyName != null) uri.queryParam(keyName, key);
        new TreeMap<>(params).forEach((name, value) -> {
            if (value instanceof Collection<?> values) uri.queryParam(name, values);
            else uri.queryParam(name, value);
        });
        return restClient.get().uri(uri.build().encode().toUri()).retrieve().body(JsonNode.class);
    }

    private Optional<JsonNode> cached(String provider, String key, Supplier<JsonNode> fetch) {
        String cacheKey = "profile-v1-" + provider + "-" + key;
        Optional<JsonNode> hit = cache.get(cacheKey, JsonNode.class);
        if (hit.isPresent()) return hit;
        try {
            JsonNode result = fetch.get();
            if (result == null || result.isNull()) return Optional.empty();
            cache.put(cacheKey, provider, result, 3600);
            return Optional.of(result);
        } catch (Exception e) {
            // Never log request URLs: they can contain provider credentials.
            log.warn("{} profile request failed ({})", provider, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
