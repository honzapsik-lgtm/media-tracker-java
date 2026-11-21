package com.mediatracker.controller;

import com.mediatracker.client.AnilistClient;
import com.mediatracker.client.IgdbClient;
import com.mediatracker.client.TmdbClient;
import com.mediatracker.model.dto.DiscoverItemDto;
import com.mediatracker.model.entity.GlobalRankingEntity;
import com.mediatracker.model.entity.MediaStatsEntity;
import com.mediatracker.repository.GlobalRankingRepository;
import com.mediatracker.repository.MediaStatsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/discover")
@Tag(name = "Discover", description = "Multi-provider media discovery endpoints")
public class DiscoverController {

    private final TmdbClient tmdbClient;
    private final IgdbClient igdbClient;
    private final AnilistClient anilistClient;
    private final MediaStatsRepository mediaStatsRepository;
    private final GlobalRankingRepository globalRankingRepository;

    private static final Map<String, Integer> TMDB_MOVIE_GENRES = Map.ofEntries(
            Map.entry("action", 28),
            Map.entry("adventure", 12),
            Map.entry("animation", 16),
            Map.entry("comedy", 35),
            Map.entry("crime", 80),
            Map.entry("documentary", 99),
            Map.entry("drama", 18),
            Map.entry("family", 10751),
            Map.entry("fantasy", 14),
            Map.entry("history", 36),
            Map.entry("horror", 27),
            Map.entry("music", 10402),
            Map.entry("mystery", 9648),
            Map.entry("romance", 10749),
            Map.entry("scifi", 878),
            Map.entry("sciencefiction", 878),
            Map.entry("tvmovie", 10770),
            Map.entry("thriller", 53),
            Map.entry("war", 10752),
            Map.entry("western", 37)
    );

    private static final Map<String, Integer> TMDB_SHOW_GENRES = Map.ofEntries(
            Map.entry("action", 10759),
            Map.entry("adventure", 10759),
            Map.entry("actionadventure", 10759),
            Map.entry("animation", 16),
            Map.entry("comedy", 35),
            Map.entry("crime", 80),
            Map.entry("documentary", 99),
            Map.entry("drama", 18),
            Map.entry("family", 10751),
            Map.entry("kids", 10762),
            Map.entry("mystery", 9648),
            Map.entry("news", 10763),
            Map.entry("reality", 10764),
            Map.entry("scifi", 10765),
            Map.entry("fantasy", 10765),
            Map.entry("scififantasy", 10765),
            Map.entry("sciencefiction", 10765),
            Map.entry("soap", 10766),
            Map.entry("talk", 10767),
            Map.entry("war", 10768),
            Map.entry("politics", 10768),
            Map.entry("warpolitics", 10768),
            Map.entry("western", 37)
    );

    public DiscoverController(TmdbClient tmdbClient,
                              IgdbClient igdbClient,
                              AnilistClient anilistClient,
                              MediaStatsRepository mediaStatsRepository,
                              GlobalRankingRepository globalRankingRepository) {
        this.tmdbClient = tmdbClient;
        this.igdbClient = igdbClient;
        this.anilistClient = anilistClient;
        this.mediaStatsRepository = mediaStatsRepository;
        this.globalRankingRepository = globalRankingRepository;
    }

    private void attachDatabaseStatsAndRanks(List<DiscoverItemDto> items) {
        if (items == null || items.isEmpty()) return;
        List<String> ids = items.stream().map(DiscoverItemDto::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return;

        Map<String, MediaStatsEntity> statsMap = mediaStatsRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(MediaStatsEntity::getId, s -> s, (a, b) -> a));

        Map<String, GlobalRankingEntity> rankMap = globalRankingRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(GlobalRankingEntity::getMediaId, r -> r, (a, b) -> a));

        for (DiscoverItemDto item : items) {
            MediaStatsEntity stats = statsMap.get(item.getId());
            if (stats != null && stats.getCommunityAverage() != null && stats.getTotalRatings() != null && stats.getTotalRatings() > 0) {
                item.setCommunityScore(stats.getCommunityAverage().intValue());
            } else {
                item.setCommunityScore(null);
            }

            GlobalRankingEntity ranking = rankMap.get(item.getId());
            if (ranking != null && ranking.getRank() != null) {
                item.setListRank(ranking.getRank());
            } else {
                item.setListRank(null);
            }
        }
    }

    /** Reads a repeatable query param (?keyword=a&keyword=b) without splitting values on commas. */
    private static List<String> multiParam(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) return List.of();
        return Arrays.stream(values).map(String::trim).filter(v -> !v.isEmpty()).distinct().limit(10).toList();
    }

    @GetMapping
    @Operation(summary = "Discover media by type, genres (repeatable), keywords (repeatable), year, sort, and page")
    public ResponseEntity<List<DiscoverItemDto>> discoverMedia(
            HttpServletRequest request,
            @RequestParam(defaultValue = "movie") String type,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page) {

        String cleanType = type.trim().toLowerCase();
        List<String> cleanGenres = multiParam(request, "genre").stream()
                .flatMap(g -> Arrays.stream(g.split(",")))
                .map(g -> g.trim().toLowerCase().replaceAll("[^a-z0-9]", ""))
                .filter(g -> !g.isEmpty()).distinct().toList();
        List<String> keywords = multiParam(request, "keyword");

        if ("movie".equals(cleanType) || "show".equals(cleanType) || "tv".equals(cleanType)) {
            boolean isMovie = "movie".equals(cleanType);
            Map<String, Integer> genreMap = isMovie ? TMDB_MOVIE_GENRES : TMDB_SHOW_GENRES;
            List<Integer> tmdbGenreIds = cleanGenres.stream().map(genreMap::get).filter(Objects::nonNull).distinct().toList();
            List<DiscoverItemDto> items = tmdbClient.discover(isMovie ? "movie" : "tv", tmdbGenreIds, keywords, year, sort, page);
            attachDatabaseStatsAndRanks(items);
            return ResponseEntity.ok(items);
        } else if ("game".equals(cleanType)) {
            List<DiscoverItemDto> items = igdbClient.discover(cleanGenres, keywords, year, sort, page);
            attachDatabaseStatsAndRanks(items);
            return ResponseEntity.ok(items);
        } else if ("manga".equals(cleanType)) {
            List<DiscoverItemDto> items = anilistClient.discoverManga(cleanGenres, keywords, year, sort, page);
            attachDatabaseStatsAndRanks(items);
            return ResponseEntity.ok(items);
        }

        return ResponseEntity.ok(List.of());
    }
}
