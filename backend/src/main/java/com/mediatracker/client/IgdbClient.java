package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.dto.DiscoverItemDto;
import com.mediatracker.model.dto.MediaItemDto;
import com.mediatracker.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class IgdbClient {

    private static final Logger log = LoggerFactory.getLogger(IgdbClient.class);
    private static final String IGDB_BASE_URL = "https://api.igdb.com/v4";
    private static final int DETAILS_CACHE_TTL = 7 * 24 * 3600;

    private final RestClient restClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private final java.util.concurrent.locks.ReentrantLock tokenLock = new java.util.concurrent.locks.ReentrantLock();
    private volatile String cachedToken = null;
    private volatile Instant tokenExpiry = Instant.MIN;

    @Value("${app.providers.twitch.client-id:}")
    private String clientId;

    @Value("${app.providers.twitch.client-secret:}")
    private String clientSecret;

    public IgdbClient(RestClient restClient, CacheService cacheService, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public String getAccessToken() {
        // Fast lock-free path if valid in-memory token exists
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        tokenLock.lock();
        try {
            // Double-check within lock
            if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
                return cachedToken;
            }

            String cacheKey = "igdb-access-token";
            Optional<String> dbCached = cacheService.get(cacheKey, String.class);
            if (dbCached.isPresent() && !dbCached.get().isBlank()) {
                this.cachedToken = dbCached.get();
                this.tokenExpiry = Instant.now().plusSeconds(300); // 5 min safety window
                return cachedToken;
            }

            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
                log.warn("Missing Twitch Client ID or Secret for IGDB auth");
                return "";
            }

            String uri = String.format("https://id.twitch.tv/oauth2/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
                    clientId, clientSecret);

            JsonNode res = restClient.post()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

            if (res != null && res.hasNonNull("access_token")) {
                String token = res.get("access_token").asText();
                int expiresIn = res.path("expires_in").asInt(3600);
                cacheService.put(cacheKey, "igdb", token, Math.max(expiresIn - 60, 60));
                this.cachedToken = token;
                this.tokenExpiry = Instant.now().plusSeconds(Math.max(expiresIn - 120, 60));
                return token;
            }
        } catch (Exception e) {
            log.error("Failed to fetch IGDB token from Twitch: {}", e.getMessage());
        } finally {
            tokenLock.unlock();
        }

        return "";
    }

    public List<MediaItemDto> searchGames(String query) {
        if (query == null || query.isBlank()) return List.of();
        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = "igdb-search-" + normalizedQuery;

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, MediaItemDto.class));
        }

        String token = getAccessToken();
        if (token.isBlank()) return List.of();

        String bodyQuery = String.format("search \"%s\"; fields name, cover.image_id, first_release_date; limit 10;", normalizedQuery.replace("\"", "\\\""));

        try {
            JsonNode data = restClient.post()
                    .uri(IGDB_BASE_URL + "/games")
                    .header("Client-ID", clientId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(bodyQuery)
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null || !data.isArray()) return List.of();

            List<MediaItemDto> results = new ArrayList<>();
            for (JsonNode g : data) {
                int id = g.path("id").asInt();
                String name = g.path("name").asText("Untitled");
                String imgId = g.path("cover").path("image_id").asText(null);
                String image = imgId != null ? "https://images.igdb.com/igdb/image/upload/t_1080p/" + imgId + ".jpg" : null;

                String rel = null;
                if (g.hasNonNull("first_release_date")) {
                    long ts = g.get("first_release_date").asLong();
                    rel = Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate().toString();
                }

                MediaItemDto dto = new MediaItemDto();
                dto.setId("igdb-game-" + id);
                dto.setTitle(name);
                dto.setType("game");
                dto.setImage(image);
                dto.setReleaseDate(rel);
                dto.setOrigin("IGDB");
                results.add(dto);
            }

            cacheService.put(cacheKey, "igdb", results, 24 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("IGDB search error for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    public List<MediaItemDto> getTrendingGames() {
        String cacheKey = "igdb-trending-games-day";
        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, MediaItemDto.class));
        }

        String token = getAccessToken();
        if (token.isBlank()) return List.of();

        String bodyQuery = "fields name, cover.image_id, first_release_date; sort total_rating_count desc; where total_rating_count > 0; limit 10;";

        try {
            JsonNode data = restClient.post()
                    .uri(IGDB_BASE_URL + "/games")
                    .header("Client-ID", clientId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(bodyQuery)
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null || !data.isArray()) return List.of();

            List<MediaItemDto> results = new ArrayList<>();
            for (JsonNode g : data) {
                int id = g.path("id").asInt();
                String name = g.path("name").asText("Untitled");
                String imgId = g.path("cover").path("image_id").asText(null);
                String image = imgId != null ? "https://images.igdb.com/igdb/image/upload/t_1080p/" + imgId + ".jpg" : null;

                String rel = null;
                if (g.hasNonNull("first_release_date")) {
                    long ts = g.get("first_release_date").asLong();
                    rel = Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate().toString();
                }

                MediaItemDto dto = new MediaItemDto();
                dto.setId("igdb-game-" + id);
                dto.setTitle(name);
                dto.setType("game");
                dto.setImage(image);
                dto.setReleaseDate(rel);
                dto.setOrigin("IGDB");
                results.add(dto);
            }

            cacheService.put(cacheKey, "igdb", results, 24 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("IGDB trending games error: {}", e.getMessage());
            return List.of();
        }
    }

    public Optional<MediaItemDto> getGameDetails(int numericId) {
        String cacheKey = "igdb-game-" + numericId;
        Optional<MediaItemDto> cached = cacheService.get(cacheKey, MediaItemDto.class);
        if (cached.isPresent()) return cached;

        String token = getAccessToken();
        if (token.isBlank()) return Optional.empty();

        String bodyQuery = String.format("fields name, cover.image_id, summary, first_release_date, genres.name, involved_companies.company.name, involved_companies.company.id, involved_companies.developer, involved_companies.publisher, game_engines.name, platforms.name, websites.url, videos.video_id, themes.name, keywords.name; where id = %d;", numericId);

        try {
            JsonNode data = restClient.post()
                    .uri(IGDB_BASE_URL + "/games")
                    .header("Client-ID", clientId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(bodyQuery)
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null || !data.isArray() || data.isEmpty()) return Optional.empty();
            JsonNode game = data.get(0);

            String name = game.path("name").asText("Untitled");
            String coverId = game.path("cover").path("image_id").asText(null);
            String image = coverId != null ? "https://images.igdb.com/igdb/image/upload/t_1080p/" + coverId + ".jpg" : null;
            String summary = game.path("summary").asText(null);

            String rel = null;
            if (game.hasNonNull("first_release_date")) {
                long ts = game.get("first_release_date").asLong();
                rel = Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate().toString();
            }

            // Trailer
            String trailerUrl = null;
            JsonNode videos = game.path("videos");
            if (videos.isArray() && !videos.isEmpty()) {
                String vidId = null;
                for (JsonNode v : videos) {
                    if (v.path("name").asText("").toLowerCase().contains("trailer") && v.hasNonNull("video_id")) {
                        vidId = v.get("video_id").asText();
                        break;
                    }
                }
                if (vidId == null && videos.get(0).hasNonNull("video_id")) {
                    vidId = videos.get(0).get("video_id").asText();
                }
                if (vidId != null) trailerUrl = "https://www.youtube.com/embed/" + vidId;
            }

            // Involved companies
            List<Object> companies = new ArrayList<>();
            JsonNode involved = game.path("involved_companies");
            if (involved.isArray()) {
                for (JsonNode ic : involved) {
                    String compName = ic.path("company").path("name").asText(null);
                    if (compName != null) {
                        Map<String, Object> cMap = new HashMap<>();
                        cMap.put("id", "igdb-" + ic.path("company").path("id").asText());
                        cMap.put("name", compName);
                        cMap.put("isDeveloper", ic.path("developer").asBoolean(false));
                        cMap.put("isPublisher", ic.path("publisher").asBoolean(false));
                        companies.add(cMap);
                    }
                }
            }

            // Engines
            List<String> engines = new ArrayList<>();
            JsonNode engs = game.path("game_engines");
            if (engs.isArray()) {
                for (JsonNode e : engs) {
                    if (e.hasNonNull("name")) engines.add(e.get("name").asText());
                }
            }

            // Play links
            List<Object> playLinks = new ArrayList<>();
            JsonNode websites = game.path("websites");
            if (websites.isArray()) {
                for (JsonNode w : websites) {
                    String url = w.path("url").asText("");
                    Map<String, String> store = matchStore(url);
                    if (store != null) {
                        playLinks.add(store);
                    }
                }
            }

            // Genres & keywords
            List<String> genres = new ArrayList<>();
            JsonNode gNode = game.path("genres");
            if (gNode.isArray()) {
                for (JsonNode g : gNode) {
                    if (g.hasNonNull("name")) genres.add(g.get("name").asText());
                }
            }

            Set<String> keywordSet = new LinkedHashSet<>();
            if (game.path("themes").isArray()) {
                for (JsonNode t : game.path("themes")) {
                    if (t.hasNonNull("name")) keywordSet.add(t.get("name").asText());
                }
            }
            if (game.path("keywords").isArray()) {
                for (JsonNode k : game.path("keywords")) {
                    if (k.hasNonNull("name")) keywordSet.add(k.get("name").asText());
                }
            }

            // Characters
            List<Object> characters = new ArrayList<>();
            try {
                String charQuery = String.format("fields name, description, mug_shot.image_id; where games = (%d); limit 50;", numericId);
                JsonNode charData = restClient.post()
                        .uri(IGDB_BASE_URL + "/characters")
                        .header("Client-ID", clientId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(charQuery)
                        .retrieve()
                        .body(JsonNode.class);

                if (charData != null && charData.isArray()) {
                    for (JsonNode c : charData) {
                        String mug = c.path("mug_shot").path("image_id").asText(null);
                        Map<String, Object> chMap = new HashMap<>();
                        chMap.put("id", c.path("id").asText());
                        chMap.put("name", c.path("name").asText("Unknown"));
                        chMap.put("description", c.path("description").asText(null));
                        chMap.put("imageUrl", mug != null ? "https://images.igdb.com/igdb/image/upload/t_1080p/" + mug + ".jpg" : null);
                        characters.add(chMap);
                    }
                }
            } catch (Exception ce) {
                log.warn("Failed to fetch characters for game {}: {}", numericId, ce.getMessage());
            }

            MediaItemDto dto = new MediaItemDto();
            dto.setId("igdb-game-" + numericId);
            dto.setTitle(name);
            dto.setType("game");
            dto.setImage(image);
            dto.setBackdrop(image);
            dto.setDescription(summary);
            dto.setReleaseDate(rel);
            dto.setTrailerUrl(trailerUrl);
            dto.setCompanies(companies);
            dto.setEngines(engines);
            dto.setPlayLinks(playLinks);
            dto.setGenres(genres);
            dto.setKeywords(new ArrayList<>(keywordSet));
            dto.setCharacters(characters);
            dto.setOrigin("IGDB");

            cacheService.put(cacheKey, "igdb", dto, DETAILS_CACHE_TTL);
            return Optional.of(dto);
        } catch (Exception e) {
            log.error("IGDB details error for {}: {}", numericId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<DiscoverItemDto> discover(String genre, String year, String sort, int page) {
        String token = getAccessToken();
        if (token.isBlank()) return List.of();

        String cacheKey = String.format("discover-game-%s-%s-%s-%d",
                genre != null ? genre : "all", year != null ? year : "all", sort != null ? sort : "default", page);

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, DiscoverItemDto.class));
        }

        List<String> where = new ArrayList<>();
        where.add("cover != null");

        String normalizedGenre = genre != null ? genre.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        applyIgdbGenreFilter(where, normalizedGenre);

        if (year != null && year.matches("^\\d{4}$")) {
            long startTs = java.time.LocalDate.of(Integer.parseInt(year), 1, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            long endTs = java.time.LocalDate.of(Integer.parseInt(year), 12, 31).atTime(23, 59, 59).atZone(ZoneOffset.UTC).toEpochSecond();
            where.add(String.format("first_release_date >= %d & first_release_date <= %d", startTs, endTs));
        }

        String sortClause;
        if ("top_rated".equals(sort)) {
            where.add("total_rating != null & total_rating_count >= 5");
            sortClause = "sort total_rating desc;";
        } else if ("lowest".equals(sort)) {
            where.add("total_rating != null & total_rating_count >= 5");
            sortClause = "sort total_rating asc;";
        } else if ("newest".equals(sort)) {
            long now = Instant.now().getEpochSecond();
            where.add(String.format("first_release_date != null & first_release_date <= %d", now));
            sortClause = "sort first_release_date desc;";
        } else if ("oldest".equals(sort)) {
            where.add("first_release_date != null");
            sortClause = "sort first_release_date asc;";
        } else {
            where.add("total_rating_count != null");
            sortClause = "sort total_rating_count desc;";
        }

        int offset = (page - 1) * 12;
        String bodyQuery = String.format("fields id, name, cover.image_id, first_release_date, total_rating; where %s; %s offset %d; limit 12;",
                String.join(" & ", where), sortClause, offset);

        try {
            JsonNode data = restClient.post()
                    .uri(IGDB_BASE_URL + "/games")
                    .header("Client-ID", clientId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(bodyQuery)
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null || !data.isArray()) return List.of();

            List<DiscoverItemDto> results = new ArrayList<>();
            for (JsonNode g : data) {
                int id = g.path("id").asInt();
                String name = g.path("name").asText("Untitled");
                String imgId = g.path("cover").path("image_id").asText(null);
                String image = imgId != null ? "https://images.igdb.com/igdb/image/upload/t_1080p/" + imgId + ".jpg" : "";

                double totalRating = g.path("total_rating").asDouble(0.0);
                int score = (int) Math.round(totalRating);

                String rel = null;
                if (g.hasNonNull("first_release_date")) {
                    long ts = g.get("first_release_date").asLong();
                    rel = Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate().toString();
                }

                results.add(new DiscoverItemDto("igdb-game-" + id, name, image, "game", score, rel));
            }

            cacheService.put(cacheKey, "igdb", results, 6 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("IGDB discover error: {}", e.getMessage());
            return List.of();
        }
    }

    private void applyIgdbGenreFilter(List<String> where, String genre) {
        Map<String, int[]> map = Map.ofEntries(
                Map.entry("pointandclick", new int[]{2, 0}),
                Map.entry("fighting", new int[]{4, 0}),
                Map.entry("shooter", new int[]{5, 0}),
                Map.entry("music", new int[]{7, 0}),
                Map.entry("platform", new int[]{8, 0}),
                Map.entry("platformer", new int[]{8, 0}),
                Map.entry("puzzle", new int[]{9, 0}),
                Map.entry("racing", new int[]{10, 0}),
                Map.entry("rts", new int[]{11, 0}),
                Map.entry("realtimestrategyrts", new int[]{11, 0}),
                Map.entry("rpg", new int[]{12, 0}),
                Map.entry("roleplaying", new int[]{12, 0}),
                Map.entry("roleplayingrpg", new int[]{12, 0}),
                Map.entry("simulator", new int[]{13, 0}),
                Map.entry("simulation", new int[]{13, 0}),
                Map.entry("sport", new int[]{14, 0}),
                Map.entry("sports", new int[]{14, 0}),
                Map.entry("strategy", new int[]{15, 0}),
                Map.entry("tactical", new int[]{24, 0}),
                Map.entry("hackandslashbeatemup", new int[]{25, 0}),
                Map.entry("arcade", new int[]{33, 0}),
                Map.entry("visualnovel", new int[]{34, 0}),
                Map.entry("action", new int[]{0, 1}),
                Map.entry("fantasy", new int[]{0, 17}),
                Map.entry("scifi", new int[]{0, 18}),
                Map.entry("horror", new int[]{0, 19}),
                Map.entry("thriller", new int[]{0, 20}),
                Map.entry("survival", new int[]{0, 21}),
                Map.entry("historical", new int[]{0, 22}),
                Map.entry("stealth", new int[]{0, 23}),
                Map.entry("comedy", new int[]{0, 27}),
                Map.entry("drama", new int[]{0, 31}),
                Map.entry("openworld", new int[]{0, 38}),
                Map.entry("warfare", new int[]{0, 39}),
                Map.entry("mystery", new int[]{0, 43}),
                Map.entry("romance", new int[]{0, 44})
        );

        int[] ids = map.get(genre);
        if (ids != null) {
            int gId = ids[0];
            int tId = ids[1];
            if (gId > 0 && tId > 0) {
                where.add(String.format("(genres = (%d) | themes = (%d))", gId, tId));
            } else if (gId > 0) {
                where.add(String.format("genres = (%d)", gId));
            } else if (tId > 0) {
                where.add(String.format("themes = (%d)", tId));
            }
        }
    }

    private Map<String, String> matchStore(String url) {
        if (url == null) return null;
        if (url.contains("steampowered.com") || url.contains("steamcommunity.com")) {
            return Map.of("site", "Steam", "url", url, "color", "#66c0f4");
        } else if (url.contains("gog.com")) {
            return Map.of("site", "GOG.com", "url", url, "color", "#bf00ff");
        } else if (url.contains("epicgames.com")) {
            return Map.of("site", "Epic Games", "url", url, "color", "#ffffff");
        } else if (url.contains("playstation.com")) {
            return Map.of("site", "PlayStation Store", "url", url, "color", "#003087");
        } else if (url.contains("xbox.com")) {
            return Map.of("site", "Xbox Store", "url", url, "color", "#107c10");
        } else if (url.contains("nintendo.com") || url.contains("nintendo.co")) {
            return Map.of("site", "Nintendo eShop", "url", url, "color", "#e60012");
        } else if (url.contains("itch.io")) {
            return Map.of("site", "itch.io", "url", url, "color", "#fa5c5c");
        } else if (url.contains("apple.com") || url.contains("apps.apple.com")) {
            return Map.of("site", "App Store", "url", url, "color", "#007aff");
        } else if (url.contains("play.google.com")) {
            return Map.of("site", "Google Play", "url", url, "color", "#00c6ff");
        }
        return null;
    }
}
