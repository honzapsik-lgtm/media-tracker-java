package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.dto.MediaCreditDto;
import com.mediatracker.model.dto.MediaItemDto;
import com.mediatracker.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class RawgClient {

    private static final Logger log = LoggerFactory.getLogger(RawgClient.class);
    private static final String BASE_URL = "https://api.rawg.io/api";
    private static final int CACHE_TTL_SECONDS = 7 * 24 * 3600;

    private final RestClient restClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @Value("${app.providers.rawg.api-key:}")
    private String apiKey;

    public RawgClient(RestClient restClient, CacheService cacheService, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public List<MediaCreditDto> getGameCrew(String gameName, Integer releaseYear) {
        if (gameName == null || gameName.isBlank()) return List.of();
        String yearSuffix = releaseYear != null ? "-" + releaseYear : "";
        String cacheKey = "rawg-crew-" + URLEncoder.encode(gameName.toLowerCase().trim(), StandardCharsets.UTF_8) + yearSuffix;

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, MediaCreditDto.class));
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RAWG_API_KEY is not defined");
            return List.of();
        }

        try {
            String encodedName = URLEncoder.encode(gameName.trim(), StandardCharsets.UTF_8);
            String searchUrl = String.format("%s/games?key=%s&search=%s", BASE_URL, apiKey, encodedName);
            if (releaseYear != null) {
                searchUrl += String.format("&dates=%d-01-01,%d-12-31", releaseYear, releaseYear);
            }

            JsonNode searchData = restClient.get().uri(searchUrl).retrieve().body(JsonNode.class);
            if (searchData == null || !searchData.has("results") || searchData.get("results").isEmpty()) {
                if (releaseYear != null) {
                    searchUrl = String.format("%s/games?key=%s&search=%s", BASE_URL, apiKey, encodedName);
                    searchData = restClient.get().uri(searchUrl).retrieve().body(JsonNode.class);
                }
            }

            if (searchData == null || !searchData.has("results") || searchData.get("results").isEmpty()) {
                return List.of();
            }

            int rawgGameId = searchData.get("results").get(0).path("id").asInt();

            String teamUrl = String.format("%s/games/%d/development-team?key=%s", BASE_URL, rawgGameId, apiKey);
            JsonNode teamData = restClient.get().uri(teamUrl).retrieve().body(JsonNode.class);

            if (teamData == null || !teamData.has("results") || !teamData.get("results").isArray()) {
                return List.of();
            }

            List<String> keyRoles = List.of("director", "writer", "composer", "design");
            List<MediaCreditDto> crew = new ArrayList<>();

            for (JsonNode member : teamData.get("results")) {
                List<String> matchedRoles = new ArrayList<>();
                JsonNode positions = member.path("positions");
                if (positions.isArray()) {
                    for (JsonNode pos : positions) {
                        String slug = pos.path("slug").asText("").toLowerCase();
                        String name = pos.path("name").asText("").toLowerCase();
                        for (String kr : keyRoles) {
                            if (slug.contains(kr) || name.contains(kr)) {
                                matchedRoles.add(pos.path("name").asText(""));
                                break;
                            }
                        }
                    }
                }

                if (!matchedRoles.isEmpty()) {
                    String role = String.join(", ", matchedRoles);
                    crew.add(new MediaCreditDto(
                            "rawg-" + member.path("id").asText(),
                            member.path("name").asText("Unknown"),
                            null,
                            role,
                            member.path("image").asText(null)
                    ));
                }
            }

            cacheService.put(cacheKey, "rawg", crew, CACHE_TTL_SECONDS);
            return crew;
        } catch (Exception e) {
            log.warn("Error fetching RAWG crew for '{}': {}", gameName, e.getMessage());
            return List.of();
        }
    }

    public Optional<MediaItemDto> getGameDetails(int id) {
        String cacheKey = "rawg-game-" + id;
        Optional<MediaItemDto> cached = cacheService.get(cacheKey, MediaItemDto.class);
        if (cached.isPresent()) return cached;

        if (apiKey == null || apiKey.isBlank()) return Optional.empty();

        try {
            String uri = String.format("%s/games/%d?key=%s", BASE_URL, id, apiKey);
            JsonNode game = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (game == null) return Optional.empty();

            String name = game.path("name").asText("Untitled");
            String bg = game.path("background_image").asText(null);
            String bgAdd = game.path("background_image_additional").asText(null);
            String desc = game.hasNonNull("description_raw") ? game.get("description_raw").asText() : game.path("description").asText("");
            String rel = game.path("released").asText("N/A");
            int meta = game.path("metacritic").asInt(0);
            int playtime = game.path("playtime").asInt(0);

            List<Object> companies = new ArrayList<>();
            JsonNode devs = game.path("developers");
            if (devs.isArray()) {
                for (JsonNode d : devs) {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", "rawg-dev-" + d.path("id").asText());
                    c.put("name", d.path("name").asText());
                    c.put("isDeveloper", true);
                    c.put("isPublisher", false);
                    companies.add(c);
                }
            }
            JsonNode pubs = game.path("publishers");
            if (pubs.isArray()) {
                for (JsonNode p : pubs) {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", "rawg-pub-" + p.path("id").asText());
                    c.put("name", p.path("name").asText());
                    c.put("isDeveloper", false);
                    c.put("isPublisher", true);
                    companies.add(c);
                }
            }

            List<Object> playLinks = new ArrayList<>();
            JsonNode stores = game.path("stores");
            if (stores.isArray()) {
                for (JsonNode s : stores) {
                    JsonNode store = s.path("store");
                    if (!store.isMissingNode()) {
                        String sName = store.path("name").asText();
                        String url = s.path("url").asText(null);
                        if (url == null || url.isBlank()) {
                            url = "https://" + store.path("domain").asText("rawg.io");
                        }
                        Map<String, String> pLink = new HashMap<>();
                        pLink.put("site", sName);
                        pLink.put("url", url);
                        pLink.put("color", "#66c0f4");
                        playLinks.add(pLink);
                    }
                }
            }

            List<String> genres = new ArrayList<>();
            JsonNode gNode = game.path("genres");
            if (gNode.isArray()) {
                for (JsonNode g : gNode) {
                    if (g.hasNonNull("name")) genres.add(g.get("name").asText());
                }
            }

            List<String> keywords = new ArrayList<>();
            JsonNode tags = game.path("tags");
            if (tags.isArray()) {
                for (JsonNode t : tags) {
                    if (t.hasNonNull("name")) keywords.add(t.get("name").asText());
                }
            }

            MediaItemDto dto = new MediaItemDto();
            dto.setId("rawg-game-" + id);
            dto.setTitle(name);
            dto.setType("game");
            dto.setImage(bg);
            dto.setBackdrop(bgAdd != null ? bgAdd : bg);
            dto.setDescription(desc);
            dto.setReleaseDate(rel);
            dto.setGlobalScore(meta);
            dto.setRuntime(playtime > 0 ? playtime : null);
            dto.setGenres(genres);
            dto.setKeywords(keywords);
            dto.setCompanies(companies);
            dto.setPlayLinks(playLinks);
            dto.setOrigin("RAWG");

            cacheService.put(cacheKey, "rawg", dto, CACHE_TTL_SECONDS);
            return Optional.of(dto);
        } catch (Exception e) {
            log.warn("RAWG details error for {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Integer> resolveRAWGToIGDB(int rawgId, IgdbClient igdbClient) {
        String cacheKey = "rawg-to-igdb-" + rawgId;
        Optional<Integer> cached = cacheService.get(cacheKey, Integer.class);
        if (cached.isPresent()) return cached;

        Optional<MediaItemDto> rawgGame = getGameDetails(rawgId);
        if (rawgGame.isEmpty() || rawgGame.get().getTitle() == null) return Optional.empty();

        List<MediaItemDto> igdbGames = igdbClient.searchGames(rawgGame.get().getTitle());
        if (!igdbGames.isEmpty()) {
            MediaItemDto match = igdbGames.stream()
                    .filter(g -> g.getTitle().equalsIgnoreCase(rawgGame.get().getTitle()))
                    .findFirst()
                    .orElse(igdbGames.get(0));

            int igdbId = Integer.parseInt(match.getId().replace("igdb-game-", ""));
            cacheService.put(cacheKey, "rawg", igdbId, 30 * 24 * 3600);
            return Optional.of(igdbId);
        }

        return Optional.empty();
    }
}
