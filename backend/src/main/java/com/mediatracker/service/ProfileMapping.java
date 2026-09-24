package com.mediatracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mediatracker.model.dto.ProfileMediaDto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class ProfileMapping {
    private ProfileMapping() {}
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    record Slug(String provider, String id) { int numericId() { return Integer.parseInt(id); } }

    static Slug parse(String slug, boolean company) {
        if (slug == null) return null;
        String value = (company ? slug : slug.trim()).toLowerCase(Locale.ROOT);
        if (!company) {
            if (value.matches("(?:mangadex-)?" + UUID)) return new Slug("mangadex", value.replaceFirst("^mangadex-", ""));
            if (value.matches("[0-9]+")) value = "tmdb-" + value;
        }
        var matcher = Pattern.compile(company ? "^(tmdb|tmdbnet|anilist|igdb|mal)(?:-[a-z]+)?-([0-9]+)$"
                : "^(tmdb|anilist|igdb|mal|rawg)(?:-[a-z]+)?-([0-9]+)$").matcher(value);
        if (!matcher.matches()) return null;
        try {
            int id = Integer.parseInt(matcher.group(2));
            return id > 0 ? new Slug(matcher.group(1), String.valueOf(id)) : null;
        } catch (NumberFormatException e) { return null; }
    }

    static String text(JsonNode node, String... path) {
        for (String part : path) node = node.path(part);
        String value = node.asText(null);
        return value == null || value.isEmpty() ? null : value;
    }
    static String first(String... values) {
        for (String value : values) if (value != null && !value.isEmpty()) return value;
        return null;
    }
    static List<JsonNode> items(JsonNode node) {
        List<JsonNode> result = new ArrayList<>();
        if (node.isArray()) node.forEach(result::add);
        return result;
    }
    static Integer year(String date) {
        try { return date == null ? null : Integer.valueOf(date.substring(0, 4)); }
        catch (RuntimeException e) { return null; }
    }
    static Integer number(JsonNode node) { return node.asInt(0) == 0 ? null : node.asInt(); }
    static String epochDate(JsonNode node) {
        return node.asLong(0) == 0 ? null : Instant.ofEpochSecond(node.asLong()).atZone(ZoneOffset.UTC).toLocalDate().toString();
    }
    static String anilistDate(JsonNode node) {
        if (number(node.path("year")) == null) return null;
        return String.format(Locale.ROOT, "%04d-%02d-%02d", node.path("year").asInt(),
                Math.max(1, node.path("month").asInt()), Math.max(1, node.path("day").asInt()));
    }
    static String tmdbImage(String path) { return path == null ? null : "https://image.tmdb.org/t/p/w500" + path; }
    static String igdbImage(String id) { return id == null ? null : "https://images.igdb.com/igdb/image/upload/t_1080p/" + id + ".jpg"; }
    static ProfileMediaDto tmdbMedia(JsonNode n, boolean tv) {
        return new ProfileMediaDto("tmdb-" + (tv ? "tv-" : "movie-") + n.path("id").asText(), tv ? "SHOW" : "MOVIE",
                first(text(n, "title"), text(n, "name"), "Unknown"), tmdbImage(text(n, "poster_path")),
                year(first(text(n, "release_date"), text(n, "first_air_date"))));
    }
    static ProfileMediaDto anilistMedia(JsonNode n) {
        boolean manga = "MANGA".equals(text(n, "type"));
        return new ProfileMediaDto("anilist-" + (manga ? "manga-" : "show-") + n.path("id").asText(), manga ? "MANGA" : "ANIME",
                first(text(n, "title", "english"), text(n, "title", "romaji"), "Unknown"), text(n, "coverImage", "large"),
                number(n.path("startDate").path("year")));
    }
    static ProfileMediaDto igdbMedia(JsonNode n) {
        return new ProfileMediaDto("igdb-game-" + n.path("id").asText(), "GAME", first(text(n, "name"), "Unknown"),
                igdbImage(text(n, "cover", "image_id")), year(epochDate(n.path("first_release_date"))));
    }
    static final String ANILIST_MEDIA = "id type format title { english romaji } coverImage { large } startDate { year }";
}
