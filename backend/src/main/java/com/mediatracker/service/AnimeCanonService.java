package com.mediatracker.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnimeCanonService {

    public record CanonSpecialRule(int targetSeason, List<Integer> specialEpisodes) {}

    public record CanonMovieItem(String id, int tmdbId, String title, int orderAfterSeason, String releaseDate) {}

    private static final Map<String, CanonSpecialRule> CANON_SPECIAL_RULES = Map.of(
            // Attack on Titan: The Final Chapters Special 1 & 2 (Season 0, Ep 36 & 37) finish Season 4
            "1429", new CanonSpecialRule(4, List.of(36, 37))
    );

    private static final Map<String, List<CanonMovieItem>> CANON_FRANCHISE_MOVIES = Map.of(
            // Jujutsu Kaisen -> Jujutsu Kaisen 0 (Prequel movie)
            "95479", List.of(new CanonMovieItem("tmdb-movie-810693", 810693, "Jujutsu Kaisen 0", 0, null)),
            // Demon Slayer -> Mugen Train (after S1) & Infinity Castle movie trilogy (after S4/S5)
            "85937", List.of(
                    new CanonMovieItem("tmdb-movie-635302", 635302, "Demon Slayer: Kimetsu no Yaiba - The Movie: Mugen Train", 1, null),
                    new CanonMovieItem("tmdb-movie-1311031", 1311031, "Demon Slayer: Kimetsu no Yaiba Infinity Castle", 5, null)
            )
    );

    public List<Map<String, Object>> getAdjustedSeasons(String tmdbId, List<Map<String, Object>> seasons) {
        if (seasons == null || seasons.isEmpty()) return List.of();
        CanonSpecialRule rule = CANON_SPECIAL_RULES.get(tmdbId);

        List<Map<String, Object>> adjusted = new ArrayList<>();
        for (Map<String, Object> s : seasons) {
            Map<String, Object> copy = new HashMap<>(s);
            int sNum = s.containsKey("season_number") && s.get("season_number") instanceof Number n ? n.intValue() : -1;
            int epCount = s.containsKey("episode_count") && s.get("episode_count") instanceof Number n ? n.intValue() : 0;

            if (rule != null && sNum == rule.targetSeason()) {
                copy.put("episode_count", epCount + rule.specialEpisodes().size());
            } else if (rule != null && sNum == 0) {
                copy.put("episode_count", Math.max(0, epCount - rule.specialEpisodes().size()));
                if ("Specials".equals(s.get("name"))) {
                    copy.put("name", "Specials & OVAs");
                }
            }
            adjusted.add(copy);
        }
        return adjusted;
    }

    public List<Integer> getCanonFinaleEpisodeNumbers(String tmdbId, int seasonNumber) {
        CanonSpecialRule rule = CANON_SPECIAL_RULES.get(tmdbId);
        if (rule != null && rule.targetSeason() == seasonNumber) {
            return rule.specialEpisodes();
        }
        return List.of();
    }

    public List<Integer> getExcludedSeason0EpisodeNumbers(String tmdbId) {
        CanonSpecialRule rule = CANON_SPECIAL_RULES.get(tmdbId);
        return rule != null ? rule.specialEpisodes() : List.of();
    }

    public List<CanonMovieItem> getCanonMoviesForShow(String tmdbId) {
        return CANON_FRANCHISE_MOVIES.getOrDefault(tmdbId, List.of());
    }
}
