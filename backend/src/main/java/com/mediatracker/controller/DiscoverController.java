package com.mediatracker.controller;

import com.mediatracker.client.AnilistClient;
import com.mediatracker.client.IgdbClient;
import com.mediatracker.client.TmdbClient;
import com.mediatracker.model.dto.DiscoverItemDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discover")
@Tag(name = "Discover", description = "Multi-provider media discovery endpoints")
public class DiscoverController {

    private final TmdbClient tmdbClient;
    private final IgdbClient igdbClient;
    private final AnilistClient anilistClient;

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

    public DiscoverController(TmdbClient tmdbClient, IgdbClient igdbClient, AnilistClient anilistClient) {
        this.tmdbClient = tmdbClient;
        this.igdbClient = igdbClient;
        this.anilistClient = anilistClient;
    }

    @GetMapping
    @Operation(summary = "Discover media by type, genre, year, sort, and page")
    public ResponseEntity<List<DiscoverItemDto>> discoverMedia(
            @RequestParam(defaultValue = "movie") String type,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page) {

        String cleanType = type.trim().toLowerCase();
        String cleanGenre = genre != null ? genre.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "";

        if ("movie".equals(cleanType) || "show".equals(cleanType) || "tv".equals(cleanType)) {
            boolean isMovie = "movie".equals(cleanType);
            Integer tmdbGenreId = isMovie ? TMDB_MOVIE_GENRES.get(cleanGenre) : TMDB_SHOW_GENRES.get(cleanGenre);
            List<DiscoverItemDto> items = tmdbClient.discover(isMovie ? "movie" : "tv", tmdbGenreId, year, sort, page);
            return ResponseEntity.ok(items);
        } else if ("game".equals(cleanType)) {
            List<DiscoverItemDto> items = igdbClient.discover(cleanGenre, year, sort, page);
            return ResponseEntity.ok(items);
        } else if ("manga".equals(cleanType)) {
            List<DiscoverItemDto> items = anilistClient.discoverManga(cleanGenre, year, sort, page);
            return ResponseEntity.ok(items);
        }

        return ResponseEntity.ok(List.of());
    }
}
