package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.dto.MediaCreditDto;
import com.mediatracker.model.dto.MediaItemDto;
import com.mediatracker.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class MangaDexClient {

    private static final Logger log = LoggerFactory.getLogger(MangaDexClient.class);
    private static final String BASE_URL = "https://api.mangadex.org";
    private static final int CACHE_TTL_SECONDS = 7 * 24 * 3600; // 7 days

    private final RestClient restClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    public MangaDexClient(RestClient restClient, CacheService cacheService, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public Optional<String> getMangaDexCoverUrl(String mangadexId) {
        if (mangadexId == null || mangadexId.isBlank()) return Optional.empty();
        try {
            String uri = String.format("%s/manga/%s?includes[]=cover_art", BASE_URL, mangadexId);
            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data == null || !data.has("data")) return Optional.empty();

            JsonNode manga = data.get("data");
            JsonNode relationships = manga.path("relationships");
            if (relationships.isArray()) {
                for (JsonNode rel : relationships) {
                    if ("cover_art".equalsIgnoreCase(rel.path("type").asText())) {
                        String fileName = rel.path("attributes").path("fileName").asText(null);
                        if (fileName != null) {
                            return Optional.of(String.format("https://uploads.mangadex.org/covers/%s/%s", mangadexId, fileName));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch MangaDex cover for {}: {}", mangadexId, e.getMessage());
        }
        return Optional.empty();
    }

    public List<MediaItemDto> searchManga(String query) {
        if (query == null || query.isBlank()) return List.of();
        String normalizedQuery = query.trim();
        String cacheKey = "mangadex-search-" + URLEncoder.encode(normalizedQuery.toLowerCase(), StandardCharsets.UTF_8);

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, MediaItemDto.class));
        }

        try {
            String encoded = URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8);
            String uri = String.format("%s/manga?title=%s&limit=15&includes[]=cover_art&includes[]=author&order[relevance]=desc&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica",
                    BASE_URL, encoded);

            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data == null || !data.has("data") || !data.get("data").isArray()) return List.of();

            List<MediaItemDto> results = new ArrayList<>();
            for (JsonNode manga : data.get("data")) {
                String id = manga.path("id").asText();
                JsonNode titles = manga.path("attributes").path("title");
                String title = titles.hasNonNull("en") ? titles.get("en").asText()
                        : titles.hasNonNull("ja-ro") ? titles.get("ja-ro").asText()
                        : titles.elements().hasNext() ? titles.elements().next().asText("Unknown Title")
                        : "Unknown Title";

                // Filter doujinshi unless explicitly requested
                boolean isDoujin = false;
                JsonNode tags = manga.path("attributes").path("tags");
                if (tags.isArray()) {
                    for (JsonNode t : tags) {
                        String tagName = t.path("attributes").path("name").path("en").asText("");
                        if ("doujinshi".equalsIgnoreCase(tagName)) {
                            isDoujin = true;
                            break;
                        }
                    }
                }
                if (isDoujin && !normalizedQuery.toLowerCase().contains("doujin")) {
                    continue;
                }

                String image = null;
                JsonNode rels = manga.path("relationships");
                if (rels.isArray()) {
                    for (JsonNode r : rels) {
                        if ("cover_art".equalsIgnoreCase(r.path("type").asText())) {
                            String fn = r.path("attributes").path("fileName").asText(null);
                            if (fn != null) {
                                image = String.format("https://uploads.mangadex.org/covers/%s/%s.512.jpg", id, fn);
                                break;
                            }
                        }
                    }
                }

                String year = manga.path("attributes").path("year").asText(null);
                String releaseDate = (year != null && !year.isBlank() && !"null".equals(year)) ? year + "-01-01" : null;

                MediaItemDto item = new MediaItemDto();
                item.setId("mangadex-manga-" + id);
                item.setTitle(title);
                item.setType("manga");
                item.setImage(image);
                item.setReleaseDate(releaseDate);
                item.setOrigin("MANGADEX");
                results.add(item);
            }

            cacheService.put(cacheKey, "mangadex", results, 24 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("MangaDex search error for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    public Optional<MediaItemDto> getMangaDetails(String mangadexId) {
        if (mangadexId == null || mangadexId.isBlank()) return Optional.empty();
        String cacheKey = "mangadex-details-" + mangadexId;

        Optional<MediaItemDto> cached = cacheService.get(cacheKey, MediaItemDto.class);
        if (cached.isPresent()) return cached;

        try {
            String uri = String.format("%s/manga/%s?includes[]=cover_art&includes[]=author&includes[]=artist", BASE_URL, mangadexId);
            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data == null || !data.has("data")) return Optional.empty();

            JsonNode manga = data.get("data");
            JsonNode attributes = manga.path("attributes");

            JsonNode titles = attributes.path("title");
            String title = titles.hasNonNull("en") ? titles.get("en").asText()
                    : titles.hasNonNull("ja-ro") ? titles.get("ja-ro").asText()
                    : titles.elements().hasNext() ? titles.elements().next().asText("Unknown Title")
                    : "Unknown Title";

            JsonNode descObj = attributes.path("description");
            String description = descObj.hasNonNull("en") ? descObj.get("en").asText()
                    : descObj.elements().hasNext() ? descObj.elements().next().asText("")
                    : "";

            String image = null;
            List<MediaCreditDto> staff = new ArrayList<>();
            JsonNode rels = manga.path("relationships");
            if (rels.isArray()) {
                for (JsonNode r : rels) {
                    String type = r.path("type").asText();
                    if ("cover_art".equalsIgnoreCase(type)) {
                        String fn = r.path("attributes").path("fileName").asText(null);
                        if (fn != null) {
                            image = String.format("https://uploads.mangadex.org/covers/%s/%s.512.jpg", mangadexId, fn);
                        }
                    } else if ("author".equalsIgnoreCase(type) || "artist".equalsIgnoreCase(type)) {
                        String authorName = r.path("attributes").path("name").asText("Unknown");
                        staff.add(new MediaCreditDto(
                                "mangadex-" + r.path("id").asText(),
                                authorName,
                                null,
                                "author".equalsIgnoreCase(type) ? "Author" : "Artist",
                                null
                        ));
                    }
                }
            }

            String year = attributes.path("year").asText(null);
            String releaseDate = (year != null && !year.isBlank() && !"null".equals(year)) ? year + "-01-01" : null;
            String status = attributes.path("status").asText(null);

            Integer chapters = attributes.hasNonNull("lastChapter") ? parseIntOrNull(attributes.get("lastChapter").asText()) : null;
            Integer volumes = attributes.hasNonNull("lastVolume") ? parseIntOrNull(attributes.get("lastVolume").asText()) : null;

            List<String> macroGenres = new ArrayList<>();
            List<String> baseGenres = new ArrayList<>();
            List<String> themes = new ArrayList<>();

            JsonNode tags = attributes.path("tags");
            if (tags.isArray()) {
                for (JsonNode t : tags) {
                    String group = t.path("attributes").path("group").asText();
                    String name = t.path("attributes").path("name").path("en").asText(null);
                    if (name != null) {
                        if ("genre".equalsIgnoreCase(group)) macroGenres.add(name);
                        if (!"format".equalsIgnoreCase(group) && !"content".equalsIgnoreCase(group)) baseGenres.add(name);
                        if ("theme".equalsIgnoreCase(group)) themes.add(name);
                    }
                }
            }

            List<String> fullGenreList = !macroGenres.isEmpty() ? macroGenres : baseGenres;
            List<String> genres = fullGenreList.subList(0, Math.min(3, fullGenreList.size()));
            List<String> overflowGenres = fullGenreList.size() > 3 ? fullGenreList.subList(3, fullGenreList.size()) : List.of();

            Set<String> keywordSet = new LinkedHashSet<>(overflowGenres);
            keywordSet.addAll(themes);

            MediaItemDto dto = new MediaItemDto();
            dto.setId("mangadex-manga-" + mangadexId);
            dto.setTitle(title);
            dto.setDescription(description);
            dto.setType("manga");
            dto.setImage(image);
            dto.setReleaseDate(releaseDate);
            dto.setStatus(status);
            dto.setChapters(chapters);
            dto.setVolumes(volumes);
            dto.setGenres(genres);
            dto.setKeywords(new ArrayList<>(keywordSet));
            dto.setCredits(staff);
            dto.setOrigin("MANGADEX");

            cacheService.put(cacheKey, "mangadex", dto, CACHE_TTL_SECONDS);
            return Optional.of(dto);
        } catch (Exception e) {
            log.warn("MangaDex details error for {}: {}", mangadexId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<MediaItemDto> getMangaByAniListId(int anilistId) {
        try {
            String uri = "https://api.malsync.moe/mal/manga/anilist:" + anilistId;
            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data != null && data.has("Sites") && data.get("Sites").has("Mangadex")) {
                JsonNode mdSites = data.get("Sites").get("Mangadex");
                Iterator<String> fieldNames = mdSites.fieldNames();
                if (fieldNames.hasNext()) {
                    String firstKey = fieldNames.next();
                    JsonNode entry = mdSites.get(firstKey);
                    String mdId = entry.hasNonNull("identifier") ? entry.get("identifier").asText() : firstKey;
                    if (mdId != null && !mdId.isBlank()) {
                        return getMangaDetails(mdId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("MAL-Sync MangaDex lookup error for anilistId {}: {}", anilistId, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<MediaItemDto> getMangaByMalId(int malId) {
        try {
            String uri = "https://api.malsync.moe/mal/manga/" + malId;
            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data != null && data.has("Sites") && data.get("Sites").has("Mangadex")) {
                JsonNode mdSites = data.get("Sites").get("Mangadex");
                Iterator<String> fieldNames = mdSites.fieldNames();
                if (fieldNames.hasNext()) {
                    String firstKey = fieldNames.next();
                    JsonNode entry = mdSites.get(firstKey);
                    String mdId = entry.hasNonNull("identifier") ? entry.get("identifier").asText() : firstKey;
                    if (mdId != null && !mdId.isBlank()) {
                        return getMangaDetails(mdId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("MAL-Sync MangaDex lookup error for malId {}: {}", malId, e.getMessage());
        }
        return Optional.empty();
    }

    private Integer parseIntOrNull(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
