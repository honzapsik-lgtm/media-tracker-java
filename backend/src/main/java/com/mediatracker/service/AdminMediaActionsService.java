package com.mediatracker.service;

import com.mediatracker.repository.ApiCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AdminMediaActionsService {
    private static final Pattern MEDIA_ID = Pattern.compile(
            "(?:tmdb-(?:movie-[0-9]+|tv-[0-9]+(?:-s[0-9]+(?:-e[0-9]+)?)?)"
            + "|(?:igdb|rawg)-game-[0-9]+|anilist-(?:manga|show)-[0-9]+"
            + "|mangadex-manga-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})");

    private final ApiCacheRepository cache;
    private final StatsService stats;

    public AdminMediaActionsService(ApiCacheRepository cache, StatsService stats) {
        this.cache = cache;
        this.stats = stats;
    }

    public static boolean isValidMediaId(String mediaId) {
        return mediaId != null && mediaId.length() <= 200 && MEDIA_ID.matcher(mediaId).matches();
    }

    private void validate(String mediaId) {
        if (!isValidMediaId(mediaId)) throw new IllegalArgumentException("Invalid or unsupported media ID");
    }

    public void refreshStats(String mediaId) {
        validate(mediaId);
        stats.refreshMediaStats(mediaId, null);
    }

    @Transactional
    public int clearCache(String mediaId) {
        validate(mediaId);
        Set<String> ids = new LinkedHashSet<>();
        for (String root : cacheRoots(mediaId)) {
            // Fetch only keys; never load large cache payloads or delete numeric-prefix neighbours.
            cache.findIdsWithPrefix(root).stream()
                    .filter(id -> id.equals(root) || id.startsWith(root + "-") || id.startsWith(root + ":"))
                    .forEach(ids::add);
        }
        return ids.isEmpty() ? 0 : cache.deleteSelectedIds(ids);
    }

    private Set<String> cacheRoots(String mediaId) {
        Set<String> roots = new LinkedHashSet<>();
        roots.add(mediaId);
        String[] parts = mediaId.split("-");
        String id = parts[2];
        switch (parts[0]) {
            case "tmdb" -> {
                if (parts.length == 3) {
                    roots.add("tmdb-details-v2-" + parts[1] + "-" + id);
                    roots.add("tmdb-rec-" + parts[1] + "-" + id);
                    if (parts[1].equals("tv")) {
                        roots.add("tmdb-episodes-" + id);
                        roots.add("tmdb-episode-credits-" + id);
                    }
                } else {
                    String season = parts[3].substring(1);
                    if (parts.length == 4) roots.add("tmdb-episodes-" + id + "-" + season);
                    roots.add("tmdb-episode-credits-" + id + "-" + season
                            + (parts.length == 5 ? "-" + parts[4].substring(1) : ""));
                }
            }
            case "anilist" -> {
                roots.add("anilist-" + id);
                roots.add("anilist-dto-" + id);
            }
            case "mangadex" -> {
                String uuid = mediaId.substring("mangadex-manga-".length());
                roots.add("mangadex-details-v2-" + uuid);
                roots.add("mangadex-chapters-" + uuid);
            }
            case "rawg" -> roots.add("rawg-to-igdb-" + id);
            default -> { }
        }
        return roots;
    }
}
