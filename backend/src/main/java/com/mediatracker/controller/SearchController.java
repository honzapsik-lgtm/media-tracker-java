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

        if ("company".equalsIgnoreCase(type) || "companies".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(searchCompanies(query));
        }

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
        List<MediaItemDto> companies = searchCompanies(query);

        List<MediaItemDto> allPeople = new ArrayList<>(tmdbPeople);
        allPeople.addAll(anilistStaff);

        List<MediaItemDto> combined = new ArrayList<>();

        // A company whose name matches the query (e.g. "mappa", "a24") goes first; others go last.
        String lowerQuery = query.toLowerCase();
        List<MediaItemDto> closeCompanies = companies.stream()
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().startsWith(lowerQuery)).limit(2).toList();
        combined.addAll(closeCompanies);

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

        companies.stream().filter(c -> !closeCompanies.contains(c)).limit(3).forEach(combined::add);

        int limit = Math.min(40, combined.size());
        return ResponseEntity.ok(combined.subList(0, limit));
    }

    /** TMDB companies plus AniList studios, without duplicate names (TMDB wins: it has logos). */
    private List<MediaItemDto> searchCompanies(String query) {
        List<MediaItemDto> result = new ArrayList<>(tmdbClient.searchCompanies(query));
        java.util.Set<String> names = new java.util.HashSet<>();
        result.forEach(c -> names.add(c.getTitle() == null ? "" : c.getTitle().trim().toLowerCase()));
        for (MediaItemDto studio : anilistClient.searchStudios(query)) {
            if (names.add(studio.getTitle() == null ? "" : studio.getTitle().trim().toLowerCase())) result.add(studio);
        }
        // Exact name matches first
        String q = query.trim().toLowerCase();
        result.sort(java.util.Comparator.comparing(c -> c.getTitle() == null || !c.getTitle().trim().equalsIgnoreCase(q)));
        return result;
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
