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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AnimeThemesClient {

    private static final Logger log = LoggerFactory.getLogger(AnimeThemesClient.class);
    private static final String BASE_URL = "https://api.animethemes.moe";
    private static final int CACHE_TTL_SECONDS = 14 * 24 * 3600; // 14 days

    private final RestClient restClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    public AnimeThemesClient(RestClient restClient, CacheService cacheService, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public Optional<AnimeThemeDto> fetchThemes(String title, String originalTitle, boolean isMovie) {
        String query = (title != null && !title.isBlank()) ? title.trim() : (originalTitle != null ? originalTitle.trim() : "");
        if (query.isBlank()) return Optional.empty();

        String queryClean = query.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String cacheKey = isMovie ? "anime-themes-movie-" + queryClean : "anime-themes-v5-" + queryClean;

        Optional<AnimeThemeDto> cached = cacheService.get(cacheKey, AnimeThemeDto.class);
        if (cached.isPresent()) {
            AnimeThemeDto c = cached.get();
            if (isMovie) {
                if (!c.getOpenings().isEmpty() || !c.getEndings().isEmpty()) return cached;
            } else {
                if (!c.getGroups().isEmpty()) return cached;
            }
        }

        List<String> searchQueries = new ArrayList<>();
        searchQueries.add(query);
        if (originalTitle != null && !originalTitle.trim().equalsIgnoreCase(query)) {
            searchQueries.add(originalTitle.trim());
        }

        for (String q : searchQueries) {
            try {
                String encodedQ = URLEncoder.encode(q, StandardCharsets.UTF_8);
                String uri = String.format("%s/anime?q=%s&include=animethemes.song.artists,animethemes.animethemeentries", BASE_URL, encodedQ);

                JsonNode data = restClient.get()
                        .uri(uri)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) MediaTracker/1.0")
                        .retrieve()
                        .body(JsonNode.class);

                if (data == null || !data.has("anime") || !data.get("anime").isArray() || data.get("anime").isEmpty()) {
                    continue;
                }

                JsonNode animeList = data.get("anime");

                // Case: Movie
                if (isMovie) {
                    JsonNode bestMovie = null;
                    for (JsonNode a : animeList) {
                        if ("Movie".equalsIgnoreCase(a.path("media_format").asText())) {
                            bestMovie = a;
                            break;
                        }
                    }
                    if (bestMovie == null) {
                        for (JsonNode a : animeList) {
                            if (a.path("name").asText("").toLowerCase().contains("movie")) {
                                bestMovie = a;
                                break;
                            }
                        }
                    }
                    if (bestMovie == null) bestMovie = animeList.get(0);

                    JsonNode animethemes = bestMovie.path("animethemes");
                    if (!animethemes.isArray() || animethemes.isEmpty()) continue;

                    List<String> openings = new ArrayList<>();
                    List<String> endings = new ArrayList<>();

                    for (JsonNode t : animethemes) {
                        String slug = t.path("slug").asText("");
                        if (slug.contains("-EN")) continue;
                        String type = t.path("type").asText("");
                        String label = formatThemeLabel(t);

                        if ("OP".equalsIgnoreCase(type) && !openings.contains(label)) {
                            openings.add(label);
                        } else if ("ED".equalsIgnoreCase(type) && !endings.contains(label)) {
                            endings.add(label);
                        }
                    }

                    if (!openings.isEmpty() || !endings.isEmpty()) {
                        AnimeThemeDto result = new AnimeThemeDto(openings, endings);
                        cacheService.put(cacheKey, "animethemes", result, CACHE_TTL_SECONDS);
                        return Optional.of(result);
                    }
                    continue;
                }

                // Case: TV franchise
                JsonNode anchor = null;
                for (JsonNode a : animeList) {
                    String format = a.path("media_format").asText();
                    String name = a.path("name").asText("").toLowerCase();
                    if (("TV".equalsIgnoreCase(format) || name.contains("kanketsu")) && a.path("animethemes").isArray() && !a.path("animethemes").isEmpty()) {
                        anchor = a;
                        break;
                    }
                }
                if (anchor == null) anchor = animeList.get(0);

                String anchorName = anchor.path("name").asText("").toLowerCase().replaceAll("[^a-z0-9\\s]", "");
                String primaryToken = "";
                for (String word : anchorName.split("\\s+")) {
                    if (word.length() >= 4) {
                        primaryToken = word;
                        break;
                    }
                }

                List<JsonNode> matchedEntries = new ArrayList<>();
                for (JsonNode a : animeList) {
                    String format = a.path("media_format").asText();
                    if ("OVA".equalsIgnoreCase(format)) continue;
                    String aName = a.path("name").asText("").toLowerCase();
                    String aSlug = a.path("slug").asText("").toLowerCase();
                    if (aName.contains("chuugakkou")) continue;

                    if (!primaryToken.isBlank() && (aName.contains(primaryToken) || aSlug.contains(primaryToken))) {
                        matchedEntries.add(a);
                    }
                }

                List<JsonNode> entriesToProcess = !matchedEntries.isEmpty() ? matchedEntries : List.of(animeList.get(0));

                Set<String> allOpeningsSet = new LinkedHashSet<>();
                Set<String> allEndingsSet = new LinkedHashSet<>();

                Set<String> seasonalNames = new HashSet<>();
                for (JsonNode entry : entriesToProcess) {
                    SeasonGroupClassification c = classifyAnimeSeasonGroup(entry.path("name").asText(""), entry.path("media_format").asText(""));
                    if (!c.isMovie && !c.isSpecial) {
                        seasonalNames.add(c.seasonName);
                    }
                }
                boolean isSeasonalAnime = seasonalNames.size() > 1;

                List<AnimeThemeDto.AnimeThemeGroup> groups = new ArrayList<>();

                if (isSeasonalAnime) {
                    Map<String, AnimeThemeDto.AnimeThemeGroup> groupMap = new LinkedHashMap<>();

                    for (JsonNode entry : entriesToProcess) {
                        JsonNode animethemes = entry.path("animethemes");
                        if (!animethemes.isArray() || animethemes.isEmpty()) continue;

                        SeasonGroupClassification c = classifyAnimeSeasonGroup(entry.path("name").asText(""), entry.path("media_format").asText(""));
                        if (c.isSpecial) continue;

                        AnimeThemeDto.AnimeThemeGroup group = groupMap.computeIfAbsent(c.seasonName, k ->
                                new AnimeThemeDto.AnimeThemeGroup(c.seasonName, c.seasonNumber, c.order, new ArrayList<>(), new ArrayList<>())
                        );

                        for (JsonNode t : animethemes) {
                            String slug = t.path("slug").asText("");
                            if (slug.contains("-EN")) continue;
                            String type = t.path("type").asText("");
                            String label = formatThemeLabel(t);

                            if ("OP".equalsIgnoreCase(type)) {
                                if (!group.getOpenings().contains(label)) group.getOpenings().add(label);
                                allOpeningsSet.add(label);
                            } else if ("ED".equalsIgnoreCase(type)) {
                                if (!group.getEndings().contains(label)) group.getEndings().add(label);
                                allEndingsSet.add(label);
                            }
                        }
                    }

                    groups = new ArrayList<>(groupMap.values());
                    groups.removeIf(g -> g.getOpenings().isEmpty() && g.getEndings().isEmpty());
                    groups.sort(Comparator.comparingInt(g -> g.getOrder() != null ? g.getOrder() : 999));
                } else {
                    // Continuous anime (e.g. One Piece, Bleach)
                    JsonNode mainEntry = entriesToProcess.get(0);
                    for (JsonNode e : entriesToProcess) {
                        if ("TV".equalsIgnoreCase(e.path("media_format").asText()) && e.path("animethemes").isArray() && !e.path("animethemes").isEmpty()) {
                            mainEntry = e;
                            break;
                        }
                    }

                    List<JsonNode> ops = new ArrayList<>();
                    List<JsonNode> eds = new ArrayList<>();

                    JsonNode animethemes = mainEntry.path("animethemes");
                    if (animethemes.isArray()) {
                        for (JsonNode t : animethemes) {
                            String slug = t.path("slug").asText("");
                            if (slug.contains("-EN")) continue;
                            String type = t.path("type").asText("");
                            String label = formatThemeLabel(t);
                            if ("OP".equalsIgnoreCase(type)) {
                                ops.add(t);
                                allOpeningsSet.add(label);
                            } else if ("ED".equalsIgnoreCase(type)) {
                                eds.add(t);
                                allEndingsSet.add(label);
                            }
                        }
                    }

                    for (JsonNode op : ops) {
                        String rawEps = "";
                        JsonNode entries = op.path("animethemeentries");
                        if (entries.isArray() && !entries.isEmpty()) {
                            rawEps = entries.get(0).path("episodes").asText("");
                        }
                        if (rawEps.isBlank()) continue;

                        EpisodeRange opRange = parseEpisodeRange(rawEps);
                        if (opRange == null) continue;

                        List<String> gOps = List.of(formatThemeLabel(op));
                        List<String> gEds = new ArrayList<>();

                        for (JsonNode ed : eds) {
                            JsonNode edEntries = ed.path("animethemeentries");
                            boolean overlaps = false;
                            if (edEntries.isArray()) {
                                for (JsonNode ee : edEntries) {
                                    EpisodeRange er = parseEpisodeRange(ee.path("episodes").asText(""));
                                    if (er != null && rangesOverlap(er, opRange)) {
                                        overlaps = true;
                                        break;
                                    }
                                }
                            }
                            if (overlaps) {
                                String edLabel = formatThemeLabel(ed);
                                if (!gEds.contains(edLabel)) gEds.add(edLabel);
                            }
                        }

                        String cleanLabel = rawEps.contains(",") ? String.format("%d-%d", opRange.start, opRange.end) : rawEps;
                        groups.add(new AnimeThemeDto.AnimeThemeGroup(cleanLabel, null, opRange.start, new ArrayList<>(gOps), gEds));
                    }
                }

                if (!allOpeningsSet.isEmpty() || !allEndingsSet.isEmpty()) {
                    AnimeThemeDto result = new AnimeThemeDto(new ArrayList<>(allOpeningsSet), new ArrayList<>(allEndingsSet));
                    result.setGroups(groups);
                    cacheService.put(cacheKey, "animethemes", result, CACHE_TTL_SECONDS);
                    return Optional.of(result);
                }
            } catch (Exception e) {
                log.warn("Error fetching anime themes from AnimeThemes.moe for query '{}': {}", q, e.getMessage());
            }
        }

        return Optional.empty();
    }

    private String formatThemeLabel(JsonNode t) {
        String slug = t.path("slug").asText("");
        String songTitle = t.path("song").path("title").asText("Unknown");
        String artist = null;
        JsonNode artists = t.path("song").path("artists");
        if (artists.isArray() && !artists.isEmpty()) {
            artist = artists.get(0).path("name").asText(null);
        }
        return (slug.isBlank() ? "" : slug + ": ") + "\"" + songTitle + "\"" + (artist != null ? " by " + artist : "");
    }

    private static class EpisodeRange {
        final int start;
        final int end;
        EpisodeRange(int start, int end) { this.start = start; this.end = end; }
    }

    private EpisodeRange parseEpisodeRange(String str) {
        if (str == null || str.isBlank()) return null;
        Pattern p = Pattern.compile("(\\d+)(?:\\s*-\\s*(\\d+))?");
        Matcher m = p.matcher(str);
        if (!m.find()) return null;
        int start = Integer.parseInt(m.group(1));
        int end = m.group(2) != null ? Integer.parseInt(m.group(2)) : start;
        return new EpisodeRange(start, end);
    }

    private boolean rangesOverlap(EpisodeRange r1, EpisodeRange r2) {
        return Math.max(r1.start, r2.start) <= Math.min(r1.end, r2.end);
    }

    private static class SeasonGroupClassification {
        final String seasonName;
        final Integer seasonNumber;
        final int order;
        final boolean isMovie;
        final boolean isSpecial;

        SeasonGroupClassification(String seasonName, Integer seasonNumber, int order, boolean isMovie, boolean isSpecial) {
            this.seasonName = seasonName;
            this.seasonNumber = seasonNumber;
            this.order = order;
            this.isMovie = isMovie;
            this.isSpecial = isSpecial;
        }
    }

    private SeasonGroupClassification classifyAnimeSeasonGroup(String name, String format) {
        String lower = name.toLowerCase();

        if (lower.contains("chuugakkou") || lower.contains("junior high")) {
            return new SeasonGroupClassification("Junior High (Spin-off)", null, 99, false, true);
        }
        if ("Movie".equalsIgnoreCase(format) || lower.contains("movie")) {
            return new SeasonGroupClassification("Movies", null, 80, true, false);
        }
        if (lower.contains("final season") || lower.contains("kanketsu") || lower.contains("season 4") || lower.contains("4th season")) {
            return new SeasonGroupClassification("The Final Season", 4, 4, false, false);
        }
        if ("OVA".equalsIgnoreCase(format) || lower.contains("ova") || lower.contains("lost girls")) {
            return new SeasonGroupClassification("Specials & OVAs", null, 90, false, true);
        }
        if (lower.contains("season 3") || lower.contains("3rd season") || lower.contains("yuukaku")) {
            return new SeasonGroupClassification("Season 3", 3, 3, false, false);
        }
        if (lower.contains("season 2") || lower.contains("2nd season") || lower.contains("mugen ressha")) {
            return new SeasonGroupClassification("Season 2", 2, 2, false, false);
        }
        if (lower.contains("season 5") || lower.contains("5th season") || lower.contains("hashira")) {
            return new SeasonGroupClassification("Season 5", 5, 5, false, false);
        }
        if (lower.contains("season 4") || lower.contains("katanakaji")) {
            return new SeasonGroupClassification("Season 4", 4, 4, false, false);
        }

        Pattern p = Pattern.compile("(?:season|part)\\s*(\\d+)|(\\d+)(?:st|nd|rd|th)\\s*season");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            String numStr = m.group(1) != null ? m.group(1) : m.group(2);
            int sNum = Integer.parseInt(numStr);
            return new SeasonGroupClassification("Season " + sNum, sNum, sNum, false, false);
        }

        return new SeasonGroupClassification("Season 1", 1, 1, false, false);
    }
}
