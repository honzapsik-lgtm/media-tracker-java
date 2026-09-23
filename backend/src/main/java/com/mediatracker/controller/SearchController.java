package com.mediatracker.controller;

import com.mediatracker.client.AnilistClient;
import com.mediatracker.client.IgdbClient;
import com.mediatracker.client.TmdbClient;
import com.mediatracker.model.dto.MediaItemDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Multi-provider unified search endpoint")
public class SearchController {

    private final TmdbClient tmdbClient;
    private final IgdbClient igdbClient;
    private final AnilistClient anilistClient;

    public SearchController(TmdbClient tmdbClient, IgdbClient igdbClient, AnilistClient anilistClient) {
        this.tmdbClient = tmdbClient;
        this.igdbClient = igdbClient;
        this.anilistClient = anilistClient;
    }

    @GetMapping
    @Operation(summary = "Search movies, shows, games, and manga across providers")
    public ResponseEntity<List<MediaItemDto>> search(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        String query = q.trim();
        List<MediaItemDto> tmdbResults = tmdbClient.search(query);
        List<MediaItemDto> games = igdbClient.searchGames(query);
        List<MediaItemDto> manga = anilistClient.searchManga(query);

        // Interleave results so users see a balanced mix of movies, shows, games, and manga
        List<MediaItemDto> combined = new ArrayList<>();
        int maxLen = Math.max(tmdbResults.size(), Math.max(games.size(), manga.size()));

        for (int i = 0; i < maxLen; i++) {
            if (i < tmdbResults.size()) combined.add(tmdbResults.get(i));
            if (i < games.size()) combined.add(games.get(i));
            if (i < manga.size()) combined.add(manga.get(i));
        }

        int limit = Math.min(24, combined.size());
        return ResponseEntity.ok(combined.subList(0, limit));
    }
}
