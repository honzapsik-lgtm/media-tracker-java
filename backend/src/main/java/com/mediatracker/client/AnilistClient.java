package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mediatracker.model.dto.DiscoverItemDto;
import com.mediatracker.model.dto.MediaCreditDto;
import com.mediatracker.model.dto.MediaItemDto;
import com.mediatracker.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class AnilistClient {

    private static final Logger log = LoggerFactory.getLogger(AnilistClient.class);
    private static final String GRAPHQL_URL = "https://graphql.anilist.co";
    private static final int CACHE_TTL_SECONDS = 7 * 24 * 3600; // 7 days

    private final RestClient restClient;
    private final CacheService cacheService;
    private final MangaDexClient mangaDexClient;
    private final ObjectMapper objectMapper;

    public AnilistClient(RestClient restClient, CacheService cacheService, MangaDexClient mangaDexClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.cacheService = cacheService;
        this.mangaDexClient = mangaDexClient;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> getAnilistRawDetails(int anilistId) {
        String cacheKey = "anilist-" + anilistId;
        Optional<JsonNode> cached = cacheService.get(cacheKey, JsonNode.class);
        if (cached.isPresent()) return cached;

        String query = """
            query ($id: Int) {
              Media(id: $id) {
                id
                idMal
                title { romaji english }
                description
                format
                episodes
                nextAiringEpisode { episode }
                duration
                chapters
                volumes
                status
                streamingEpisodes { title thumbnail }
                coverImage { extraLarge large }
                bannerImage
                trailer { id site thumbnail }
                externalLinks { site url icon color type language }
                startDate { year month day }
                averageScore
                genres
                relations {
                  edges {
                    relationType
                    node {
                      id
                      title { romaji english }
                      format
                      episodes
                      coverImage { extraLarge large }
                    }
                  }
                }
              }
            }
        """;

        Map<String, Object> body = Map.of("query", query, "variables", Map.of("id", anilistId));

        try {
            JsonNode response = restClient.post()
                    .uri(GRAPHQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("data") && response.get("data").has("Media")) {
                JsonNode media = response.get("data").get("Media");
                if (media == null || media.isNull()) return Optional.empty();
                cacheService.put(cacheKey, "anilist", media, CACHE_TTL_SECONDS);
                return Optional.of(media);
            }
        } catch (Exception e) {
            log.warn("Anilist query failed for id {}: {}", anilistId, e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Integer> getTmdbMapping(int anilistId) {
        String key = "anilist-tmdb-mapping-" + anilistId;
        Optional<Integer> cached = cacheService.get(key, Integer.class);
        if (cached.isPresent()) return cached;
        try {
            JsonNode data = restClient.get().uri("https://api.malsync.moe/mal/anime/anilist:" + anilistId)
                    .retrieve().body(JsonNode.class);
            if (data != null) {
                var entries = data.path("Sites").path("TMDB").fields();
                if (entries.hasNext()) {
                    var entry = entries.next();
                    int id = Integer.parseInt(entry.getValue().path("id").asText(entry.getKey()));
                    if (id > 0) {
                        cacheService.put(key, "anilist", id, CACHE_TTL_SECONDS);
                        return Optional.of(id);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("TMDb mapping unavailable for AniList {}: {}", anilistId, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<MediaItemDto> getDetails(int anilistId) {
        String cacheKey = "anilist-dto-" + anilistId;
        Optional<MediaItemDto> cached = cacheService.get(cacheKey, MediaItemDto.class);
        if (cached.isPresent()) return cached;

        Optional<JsonNode> rawOpt = getAnilistRawDetails(anilistId);
        if (rawOpt.isEmpty()) return Optional.empty();
        JsonNode data = rawOpt.get();

        String format = data.path("format").asText("");
        boolean isManga = List.of("MANGA", "NOVEL", "ONE_SHOT").contains(format);

        String title = data.path("title").hasNonNull("english") ? data.path("title").get("english").asText()
                : data.path("title").hasNonNull("romaji") ? data.path("title").get("romaji").asText()
                : "Unknown Title";

        String cover = data.path("coverImage").hasNonNull("extraLarge") ? data.path("coverImage").get("extraLarge").asText()
                : data.path("coverImage").hasNonNull("large") ? data.path("coverImage").get("large").asText()
                : null;

        String backdrop = data.path("bannerImage").asText(null);

        Integer year = data.path("startDate").hasNonNull("year") ? data.path("startDate").get("year").asInt() : null;
        Integer month = data.path("startDate").hasNonNull("month") ? data.path("startDate").get("month").asInt() : 1;
        Integer day = data.path("startDate").hasNonNull("day") ? data.path("startDate").get("day").asInt() : 1;
        String releaseDate = year != null ? String.format("%04d-%02d-%02d", year, month, day) : null;

        List<String> genres = new ArrayList<>();
        if (data.has("genres") && data.get("genres").isArray()) {
            for (JsonNode g : data.get("genres")) genres.add(g.asText());
        }

        MediaItemDto dto = new MediaItemDto();
        dto.setId(isManga ? "anilist-manga-" + anilistId : "anilist-show-" + anilistId);
        dto.setTitle(title);
        dto.setOriginalTitle(data.path("title").path("romaji").asText(null));
        dto.setType(isManga ? "manga" : "show");
        dto.setImage(cover);
        dto.setBackdrop(backdrop);
        dto.setDescription(data.path("description").asText(""));
        dto.setReleaseDate(releaseDate);
        dto.setGlobalScore(data.path("averageScore").asInt(0));
        dto.setRuntime(data.path("duration").asInt(0) > 0 ? data.path("duration").asInt() : null);
        dto.setGenres(genres);
        dto.setStatus(data.path("status").asText(null));
        dto.setOrigin("ANILIST");

        if (isManga) {
            dto.setChapters(data.path("chapters").asInt(0) > 0 ? data.path("chapters").asInt() : null);
            dto.setVolumes(data.path("volumes").asInt(0) > 0 ? data.path("volumes").asInt() : null);

            // MangaDex cover augmentation check
            Optional<MediaItemDto> md = mangaDexClient.getMangaByAniListId(anilistId);
            if (md.isPresent() && md.get().getImage() != null) {
                dto.setImage(md.get().getImage());
            }
        }

        cacheService.put(cacheKey, "anilist", dto, CACHE_TTL_SECONDS);
        return Optional.of(dto);
    }

    public List<MediaItemDto> searchManga(String query) {
        if (query == null || query.isBlank()) return List.of();

        String gql = """
            query ($search: String) {
              Page(page: 1, perPage: 20) {
                media(search: $search, type: MANGA, sort: POPULARITY_DESC) {
                  id
                  title { romaji english }
                  format
                  chapters
                  volumes
                  coverImage { large }
                  startDate { year month day }
                }
              }
            }
        """;

        Map<String, Object> body = Map.of("query", gql, "variables", Map.of("search", query.trim()));

        try {
            JsonNode response = restClient.post()
                    .uri(GRAPHQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("data") && response.get("data").has("Page")) {
                JsonNode mediaList = response.get("data").get("Page").path("media");
                if (mediaList.isArray() && !mediaList.isEmpty()) {
                    List<MediaItemDto> results = new ArrayList<>();
                    for (JsonNode item : mediaList) {
                        int id = item.path("id").asInt();
                        String title = item.path("title").hasNonNull("english") ? item.path("title").get("english").asText()
                                : item.path("title").hasNonNull("romaji") ? item.path("title").get("romaji").asText()
                                : "Unknown Title";

                        String img = item.path("coverImage").path("large").asText(null);
                        Integer year = item.path("startDate").hasNonNull("year") ? item.path("startDate").get("year").asInt() : null;
                        Integer month = item.path("startDate").hasNonNull("month") ? item.path("startDate").get("month").asInt() : 1;
                        Integer day = item.path("startDate").hasNonNull("day") ? item.path("startDate").get("day").asInt() : 1;
                        String relDate = year != null ? String.format("%04d-%02d-%02d", year, month, day) : null;

                        MediaItemDto dto = new MediaItemDto();
                        dto.setId("anilist-manga-" + id);
                        dto.setTitle(title);
                        dto.setType("manga");
                        dto.setImage(img);
                        dto.setReleaseDate(relDate);
                        dto.setOrigin("ANILIST");
                        results.add(dto);
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            log.warn("AniList manga search failed, falling back to MangaDex: {}", e.getMessage());
        }

        return mangaDexClient.searchManga(query);
    }

    /** Anime studios (e.g. "MAPPA"). Ids are company slugs: anilist-<id>. */
    public List<MediaItemDto> searchStudios(String query) {
        if (query == null || query.isBlank()) return List.of();
        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = "anilist-search-studio-" + normalizedQuery;

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, MediaItemDto.class));
        }

        String gql = """
            query ($search: String) {
              Page(page: 1, perPage: 6) {
                studios(search: $search) { id name isAnimationStudio }
              }
            }
        """;
        Map<String, Object> body = Map.of("query", gql, "variables", Map.of("search", query.trim()));
        try {
            JsonNode response = restClient.post()
                    .uri(GRAPHQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode studios = response == null ? null : response.path("data").path("Page").path("studios");
            if (studios == null || !studios.isArray()) return List.of();

            List<MediaItemDto> results = new ArrayList<>();
            for (JsonNode item : studios) {
                MediaItemDto dto = new MediaItemDto();
                dto.setId("anilist-" + item.path("id").asInt());
                dto.setTitle(item.path("name").asText("Unknown"));
                dto.setType("company");
                dto.setImage(null);
                dto.setDescription(item.path("isAnimationStudio").asBoolean(false) ? "Animation studio" : "Studio");
                dto.setOrigin("ANILIST");
                results.add(dto);
            }
            cacheService.put(cacheKey, "anilist", results, 24 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("AniList studio search error for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    public List<MediaItemDto> searchStaff(String query) {
        if (query == null || query.isBlank()) return List.of();
        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = "anilist-search-staff-" + normalizedQuery;

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, MediaItemDto.class));
        }

        String gql = """
            query ($search: String) {
              Page(page: 1, perPage: 10) {
                staff(search: $search) {
                  id
                  name { full native }
                  image { large }
                  primaryOccupations
                }
              }
            }
        """;

        Map<String, Object> body = Map.of("query", gql, "variables", Map.of("search", query.trim()));

        try {
            JsonNode response = restClient.post()
                    .uri(GRAPHQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("data") && response.get("data").has("Page")) {
                JsonNode staffList = response.get("data").get("Page").path("staff");
                if (staffList.isArray() && !staffList.isEmpty()) {
                    List<MediaItemDto> results = new ArrayList<>();
                    for (JsonNode item : staffList) {
                        int id = item.path("id").asInt();
                        String name = item.path("name").hasNonNull("full") ? item.path("name").get("full").asText()
                                : item.path("name").hasNonNull("native") ? item.path("name").get("native").asText()
                                : "Unknown";

                        String img = item.path("image").path("large").asText(null);
                        List<String> occupations = new ArrayList<>();
                        JsonNode occNode = item.path("primaryOccupations");
                        if (occNode.isArray()) {
                            for (JsonNode o : occNode) {
                                occupations.add(o.asText());
                            }
                        }
                        String role = occupations.isEmpty() ? "Creator" : String.join(", ", occupations);

                        MediaItemDto dto = new MediaItemDto();
                        dto.setId("anilist-" + id);
                        dto.setTitle(name);
                        dto.setType("person");
                        dto.setImage(img);
                        dto.setReleaseDate(role);
                        dto.setDescription(role);
                        dto.setOrigin("ANILIST");
                        results.add(dto);
                    }
                    cacheService.put(cacheKey, "anilist", results, 24 * 3600);
                    return results;
                }
            }
        } catch (Exception e) {
            log.warn("AniList staff search error for query '{}': {}", query, e.getMessage());
        }
        return List.of();
    }

    private static final Set<String> ANILIST_GENRES = Set.of(
            "action", "adventure", "comedy", "drama", "ecchi", "fantasy", "hentai", "horror", "mahou shoujo",
            "mecha", "music", "mystery", "psychological", "romance", "sci-fi", "slice of life", "sports",
            "supernatural", "thriller");

    public List<DiscoverItemDto> discoverManga(String genre, String year, String sort, int page) {
        return discoverManga(genre != null && !genre.isBlank() ? List.of(genre) : List.of(), List.of(), year, sort, page);
    }

    /** Discover manga matching all given genre filters and all given keywords (AniList genres or tags, by name). */
    public List<DiscoverItemDto> discoverManga(List<String> genres, List<String> keywords, String year, String sort, int page) {

        Map<String, Map<String, String>> anilistGenreMap = Map.ofEntries(
                Map.entry("action", Map.of("genre", "Action")),
                Map.entry("adventure", Map.of("genre", "Adventure")),
                Map.entry("comedy", Map.of("genre", "Comedy")),
                Map.entry("drama", Map.of("genre", "Drama")),
                Map.entry("fantasy", Map.of("genre", "Fantasy")),
                Map.entry("horror", Map.of("genre", "Horror")),
                Map.entry("mystery", Map.of("genre", "Mystery")),
                Map.entry("psychological", Map.of("genre", "Psychological")),
                Map.entry("romance", Map.of("genre", "Romance")),
                Map.entry("scifi", Map.of("genre", "Sci-Fi")),
                Map.entry("sciencefiction", Map.of("genre", "Sci-Fi")),
                Map.entry("slice", Map.of("genre", "Slice of Life")),
                Map.entry("sliceoflife", Map.of("genre", "Slice of Life")),
                Map.entry("sports", Map.of("genre", "Sports")),
                Map.entry("supernatural", Map.of("genre", "Supernatural")),
                Map.entry("suspense", Map.of("genre", "Thriller")),
                Map.entry("thriller", Map.of("genre", "Thriller")),
                Map.entry("historical", Map.of("tag", "Historical")),
                Map.entry("martialarts", Map.of("tag", "Martial Arts")),
                Map.entry("mecha", Map.of("genre", "Mecha")),
                Map.entry("seinen", Map.of("tag", "Seinen")),
                Map.entry("shounen", Map.of("tag", "Shounen")),
                Map.entry("shoujo", Map.of("tag", "Shoujo")),
                Map.entry("josei", Map.of("tag", "Josei")),
                Map.entry("isekai", Map.of("tag", "Isekai")),
                Map.entry("ecchi", Map.of("genre", "Ecchi")),
                Map.entry("music", Map.of("genre", "Music"))
        );

        Set<String> genreIn = new TreeSet<>();
        Set<String> tagIn = new TreeSet<>();
        for (String g : genres != null ? genres : List.<String>of()) {
            Map<String, String> mapped = anilistGenreMap.get(g.trim().toLowerCase().replaceAll("[^a-z0-9]", ""));
            if (mapped == null) continue;
            if (mapped.containsKey("genre")) genreIn.add(mapped.get("genre"));
            if (mapped.containsKey("tag")) tagIn.add(mapped.get("tag"));
        }
        for (String kw : keywords != null ? keywords : List.<String>of()) {
            String name = kw.trim();
            if (name.isEmpty()) continue;
            if (ANILIST_GENRES.contains(name.toLowerCase(Locale.ROOT))) {
                // AniList genre names are case-sensitive; reuse the canonical spelling from the map where possible
                Map<String, String> mapped = anilistGenreMap.get(name.toLowerCase().replaceAll("[^a-z0-9]", ""));
                genreIn.add(mapped != null && mapped.containsKey("genre") ? mapped.get("genre") : name);
            } else {
                tagIn.add(name);
            }
        }

        String cacheKey = String.format("discover-manga-g%s-t%s-%s-%s-%d",
                genreIn.isEmpty() ? "all" : String.join(",", genreIn), tagIn.isEmpty() ? "none" : String.join(",", tagIn),
                year != null ? year : "all", sort != null ? sort : "default", page);

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, DiscoverItemDto.class));
        }

        String gql = """
            query ($genres: [String], $tags: [String], $sort: [MediaSort], $year: String, $page: Int) {
              Page(page: $page, perPage: 12) {
                media(type: MANGA, genre_in: $genres, tag_in: $tags, sort: $sort, startDate_like: $year) {
                  id
                  title { english romaji }
                  coverImage { large extraLarge }
                  averageScore
                  startDate { year month day }
                }
              }
            }
        """;

        List<String> sortList = "top_rated".equals(sort) ? List.of("SCORE_DESC")
                : "lowest".equals(sort) ? List.of("SCORE_ASC")
                : "newest".equals(sort) ? List.of("START_DATE_DESC")
                : "oldest".equals(sort) ? List.of("START_DATE_ASC")
                : List.of("POPULARITY_DESC");

        Map<String, Object> vars = new HashMap<>();
        vars.put("page", page);
        vars.put("sort", sortList);
        if (!genreIn.isEmpty()) vars.put("genres", new ArrayList<>(genreIn));
        if (!tagIn.isEmpty()) vars.put("tags", new ArrayList<>(tagIn));
        if (year != null && !year.isBlank() && year.matches("^\\d{4}$")) vars.put("year", year + "%");

        try {
            JsonNode res = restClient.post()
                    .uri(GRAPHQL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", gql, "variables", vars))
                    .retrieve()
                    .body(JsonNode.class);

            if (res != null && res.has("data") && res.get("data").has("Page")) {
                JsonNode media = res.get("data").get("Page").path("media");
                if (media.isArray()) {
                    List<DiscoverItemDto> results = new ArrayList<>();
                    for (JsonNode m : media) {
                        int id = m.path("id").asInt();
                        String title = m.path("title").hasNonNull("english") ? m.path("title").get("english").asText()
                                : m.path("title").hasNonNull("romaji") ? m.path("title").get("romaji").asText()
                                : "Untitled";

                        String img = m.path("coverImage").hasNonNull("extraLarge") ? m.path("coverImage").get("extraLarge").asText()
                                : m.path("coverImage").path("large").asText("");

                        int score = m.path("averageScore").asInt(0);

                        Integer y = m.path("startDate").hasNonNull("year") ? m.path("startDate").get("year").asInt() : null;
                        Integer mo = m.path("startDate").hasNonNull("month") ? m.path("startDate").get("month").asInt() : 1;
                        Integer d = m.path("startDate").hasNonNull("day") ? m.path("startDate").get("day").asInt() : 1;
                        String rel = y != null ? String.format("%04d-%02d-%02d", y, mo, d) : null;

                        results.add(new DiscoverItemDto("anilist-manga-" + id, title, img, "manga", score, rel));
                    }

                    cacheService.put(cacheKey, "anilist", results, 6 * 3600);
                    return results;
                }
            }
        } catch (Exception e) {
            log.warn("AniList discover manga failed: {}", e.getMessage());
        }

        return List.of();
    }
}
