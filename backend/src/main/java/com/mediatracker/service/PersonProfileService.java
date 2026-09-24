package com.mediatracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.mediatracker.client.ProfileProviderClient;
import com.mediatracker.model.dto.ProfileMediaDto;
import com.mediatracker.model.dto.UnifiedProfileDto;
import com.mediatracker.model.dto.UnifiedProfileDto.Credit;
import com.mediatracker.model.dto.UnifiedProfileDto.Credits;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.mediatracker.service.ProfileMapping.*;

@Service
public class PersonProfileService {
    private final ProfileProviderClient providers;

    public PersonProfileService(ProfileProviderClient providers) { this.providers = providers; }

    public Optional<UnifiedProfileDto> getProfile(String slug) {
        Slug parsed = parse(slug, false);
        if (parsed == null) return Optional.empty();
        return switch (parsed.provider()) {
            case "tmdb" -> tmdb(parsed.numericId());
            case "anilist" -> anilist(parsed.numericId());
            case "igdb" -> igdb(parsed.numericId());
            case "rawg" -> rawg(parsed.numericId());
            case "mangadex" -> mangadex(parsed.id());
            default -> Optional.empty();
        };
    }

    private UnifiedProfileDto profile(String provider, String id, String name, String nativeName, String bio,
            String image, String birth, String death, String department, String rawgSlug, List<Credit> cast, List<Credit> crew) {
        Integer numeric = provider.equals("mangadex") ? null : Integer.valueOf(id);
        return new UnifiedProfileDto(provider + "-" + id, provider.equals("tmdb") ? numeric : null,
                provider.equals("anilist") ? numeric : null, provider.equals("igdb") ? numeric : null,
                null, provider.equals("rawg") ? numeric : null, rawgSlug, provider.equals("mangadex") ? id : null,
                first(name, "Unknown"), nativeName, bio, image, birth, death, department, new Credits(cast, crew));
    }

    private Optional<UnifiedProfileDto> tmdb(int id) {
        return providers.tmdb("/person/" + id, Map.of("language", "en-US", "append_to_response", "combined_credits"))
                .map(data -> profile("tmdb", String.valueOf(id), text(data, "name"), text(data.path("also_known_as").path(0)),
                        text(data, "biography"), tmdbImage(text(data, "profile_path")), text(data, "birthday"), text(data, "deathday"),
                        text(data, "known_for_department"), null, tmdbCredits(data.path("combined_credits").path("cast"), true),
                        tmdbCredits(data.path("combined_credits").path("crew"), false)));
    }

    private List<Credit> tmdbCredits(JsonNode data, boolean cast) {
        List<JsonNode> sorted = items(data);
        sorted.sort(Comparator.comparingDouble((JsonNode n) -> n.path("popularity").asDouble()).reversed());
        List<Credit> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode item : sorted) {
            String type = text(item, "media_type");
            if (!"movie".equals(type) && !"tv".equals(type)) continue;
            ProfileMediaDto media = tmdbMedia(item, "tv".equals(type));
            String key = media.mediaId() + (cast ? "" : "-" + text(item, "job"));
            if (!seen.add(key)) continue;
            String role = first(text(item, cast ? "character" : "job"), cast ? "Actor" : "Crew");
            result.add(new Credit(media, role, cast && role.toLowerCase(Locale.ROOT).contains("(voice)"), null));
        }
        return result;
    }

    private Optional<UnifiedProfileDto> igdb(int id) {
        return providers.igdb("persons", id, "name, description, dob, mug_shot.image_id, credited_games.name, credited_games.cover.image_id, credited_games.first_release_date")
                .map(data -> profile("igdb", String.valueOf(id), text(data, "name"), null, text(data, "description"),
                        igdbImage(text(data, "mug_shot", "image_id")), epochDate(data.path("dob")), null, "Game Development", null,
                        List.of(), items(data.path("credited_games")).stream().map(g -> new Credit(igdbMedia(g), "Developer", false, null)).toList()));
    }

    private Optional<UnifiedProfileDto> rawg(int id) {
        return providers.rawg("/creators/" + id, Map.of()).map(data -> {
            String slug = text(data, "slug");
            List<String> positions = items(data.path("positions")).stream().map(p -> {
                String name = first(text(p, "name"), "");
                return name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
            }).toList();
            String role = "[RESOLVING_ROLE]:" + (positions.isEmpty() ? "Developer" : String.join(", ", positions));
            List<Credit> crew = new ArrayList<>();
            if (slug != null) {
                JsonNode games = providers.rawg("/games", Map.of("creators", slug, "page_size", 40)).orElse(MissingNode.getInstance());
                for (JsonNode g : items(games.path("results"))) {
                    crew.add(new Credit(new ProfileMediaDto("rawg-game-" + g.path("id").asText(), "GAME",
                            first(text(g, "name"), "Unknown"), text(g, "background_image"), year(text(g, "released"))), role, false, null));
                }
            }
            return profile("rawg", String.valueOf(id), text(data, "name"), null, text(data, "description"), text(data, "image"),
                    null, null, "Game Development", slug, List.of(), crew);
        });
    }

    private Optional<UnifiedProfileDto> mangadex(String id) {
        return providers.mangadex("/author/" + id, Map.of()).map(n -> n.path("data")).filter(n -> !n.isMissingNode() && !n.isNull()).map(data -> {
            List<JsonNode> works = new ArrayList<>();
            // Separate requests are required: MangaDex combines authors and artists with AND.
            for (String relation : List.of("authors[]", "artists[]")) {
                JsonNode response = providers.mangadex("/manga", Map.of(relation, id, "includes[]", "cover_art",
                        "order[relevance]", "desc", "limit", 100)).orElse(MissingNode.getInstance());
                works.addAll(items(response.path("data")));
            }
            Set<String> seen = new HashSet<>();
            List<Credit> crew = new ArrayList<>();
            for (JsonNode manga : works) {
                String mangaId = manga.path("id").asText();
                if (!seen.add(mangaId)) continue;
                JsonNode attr = manga.path("attributes");
                JsonNode titles = attr.path("title");
                String title = first(text(titles, "en"), text(titles, "ja-ro"), firstValue(titles), "Unknown Title");
                String file = items(manga.path("relationships")).stream().filter(n -> "cover_art".equals(text(n, "type")))
                        .findFirst().map(n -> text(n, "attributes", "fileName")).orElse(null);
                Integer releaseYear = number(attr.path("year"));
                if (releaseYear == null) releaseYear = year(text(attr, "createdAt"));
                crew.add(new Credit(new ProfileMediaDto("mangadex-manga-" + mangaId, "MANGA", title,
                        file == null ? null : "https://uploads.mangadex.org/covers/" + mangaId + "/" + file + ".512.jpg", releaseYear),
                        "Story & Art", false, null));
            }
            crew.sort(Comparator.comparingInt((Credit c) -> c.releaseYear() == null ? 0 : c.releaseYear()).reversed());
            JsonNode attr = data.path("attributes");
            JsonNode bio = attr.path("biography");
            return profile("mangadex", id, text(attr, "name"), null,
                    bio.isTextual() ? text(bio) : first(text(bio, "en"), firstValue(bio)), text(attr, "imageUrl"),
                    null, null, "Manga Creation", null, List.of(), crew);
        });
    }

    private String firstValue(JsonNode node) {
        return node.isObject() && !node.isEmpty() ? text(node.elements().next()) : null;
    }

    private String connection(String kind, String page) {
        String fields = kind.equals("characterMedia") ? "characterRole characters { name { full } image { large } }" : "staffRole";
        return kind + "(page: " + page + ", perPage: 50, sort: [POPULARITY_DESC]) { pageInfo { hasNextPage } edges { "
                + fields + " node { " + ANILIST_MEDIA + " } } }";
    }

    private Optional<UnifiedProfileDto> anilist(int id) {
        String query = "query ($id: Int) { Staff(id: $id) { id name { full native } image { large } description "
                + "dateOfBirth { year month day } dateOfDeath { year month day } primaryOccupations "
                + connection("characterMedia", "1") + " " + connection("staffMedia", "1") + " } }";
        return providers.anilist("person-" + id, query, Map.of("id", id), false)
                .map(n -> n.path("Staff")).filter(n -> !n.isNull() && !n.isMissingNode()).map(data -> {
                    List<Credit> cast = anilistCredits(id, data, true);
                    List<Credit> crew = anilistCredits(id, data, false);
                    return profile("anilist", String.valueOf(id), text(data, "name", "full"), text(data, "name", "native"),
                            text(data, "description"), text(data, "image", "large"), anilistDate(data.path("dateOfBirth")),
                            anilistDate(data.path("dateOfDeath")), text(data.path("primaryOccupations").path(0)), null, cast, crew);
                });
    }

    private List<Credit> anilistCredits(int id, JsonNode data, boolean cast) {
        String kind = cast ? "characterMedia" : "staffMedia";
        List<JsonNode> edges = items(data.path(kind).path("edges"));
        if (data.path(kind).path("pageInfo").path("hasNextPage").asBoolean()) {
            String query = "query ($id: Int, $page: Int) { Staff(id: $id) { " + connection(kind, "$page") + " } }";
            providers.anilist("person-" + id + "-" + kind + "-2", query, Map.of("id", id, "page", 2), true)
                    .ifPresent(page -> edges.addAll(items(page.path("Staff").path(kind).path("edges"))));
        }
        List<Credit> result = new ArrayList<>();
        for (JsonNode edge : edges) {
            JsonNode node = edge.path("node");
            if (node.isNull() || node.isMissingNode()) continue;
            ProfileMediaDto media = anilistMedia(node);
            JsonNode character = edge.path("characters").path(0);
            result.add(new Credit(media, cast ? first(text(character, "name", "full"), "Voice Actor")
                    : first(text(edge, "staffRole"), "Staff"), cast && !media.mediaType().equals("MANGA"),
                    cast ? text(character, "image", "large") : null));
        }
        return result;
    }
}
