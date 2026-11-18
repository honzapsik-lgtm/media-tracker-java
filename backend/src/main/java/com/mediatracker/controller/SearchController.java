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
    @Operation(summary = "Search movies, shows, games, manga, and people across providers")
    public ResponseEntity<List<MediaItemDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        String query = q.trim();

        if ("person".equalsIgnoreCase(type) || "people".equalsIgnoreCase(type)) {
            List<MediaItemDto> people = new ArrayList<>(tmdbClient.searchPersons(query));
            people.addAll(anilistClient.searchStaff(query));
            return ResponseEntity.ok(people);
        }

        List<MediaItemDto> tmdbResults = tmdbClient.search(query);
        List<MediaItemDto> tmdbPeople = tmdbClient.searchPersons(query);
        List<MediaItemDto> games = igdbClient.searchGames(query);
        List<MediaItemDto> manga = anilistClient.searchManga(query);
        List<MediaItemDto> anilistStaff = anilistClient.searchStaff(query);

        List<MediaItemDto> allPeople = new ArrayList<>(tmdbPeople);
        allPeople.addAll(anilistStaff);

        List<MediaItemDto> combined = new ArrayList<>();

        // Add top matching people right at the front so actor/person searches are immediately visible
        int topPeopleCount = Math.min(3, allPeople.size());
        for (int i = 0; i < topPeopleCount; i++) {
            combined.add(allPeople.get(i));
        }

        // Interleave results so users see a balanced mix of movies, shows, games, and manga
        int maxLen = Math.max(tmdbResults.size(), Math.max(games.size(), manga.size()));
        for (int i = 0; i < maxLen; i++) {
            if (i < tmdbResults.size()) combined.add(tmdbResults.get(i));
            if (i < games.size()) combined.add(games.get(i));
            if (i < manga.size()) combined.add(manga.get(i));
        }

        // Append remaining people
        for (int i = topPeopleCount; i < allPeople.size(); i++) {
            combined.add(allPeople.get(i));
        }

        int limit = Math.min(36, combined.size());
        return ResponseEntity.ok(combined.subList(0, limit));
    }

    @GetMapping("/people")
    @Operation(summary = "Search actors, directors, writers, and staff specifically")
    public ResponseEntity<List<MediaItemDto>> searchPeople(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        String query = q.trim();
        List<MediaItemDto> people = new ArrayList<>(tmdbClient.searchPersons(query));
        people.addAll(anilistClient.searchStaff(query));
        return ResponseEntity.ok(people);
    }
}
