package com.mediatracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.mediatracker.client.ProfileProviderClient;
import com.mediatracker.model.dto.ProfileMediaDto;
import com.mediatracker.model.dto.UnifiedCompanyProfileDto;
import com.mediatracker.model.dto.UnifiedCompanyProfileDto.Portfolio;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.mediatracker.service.ProfileMapping.*;

@Service
public class CompanyProfileService {
    private final ProfileProviderClient providers;
    public CompanyProfileService(ProfileProviderClient providers) { this.providers = providers; }

    public Optional<UnifiedCompanyProfileDto> getProfile(String slug) {
        Slug parsed = parse(slug, true);
        if (parsed == null) return Optional.empty();
        return switch (parsed.provider()) {
            case "tmdb" -> tmdb(parsed.numericId(), false);
            case "tmdbnet" -> tmdb(parsed.numericId(), true);
            case "anilist" -> anilist(parsed.numericId());
            case "igdb" -> igdb(parsed.numericId());
            default -> Optional.empty();
        };
    }

    private Optional<UnifiedCompanyProfileDto> igdb(int id) {
        return providers.igdb("companies", id, "name, description, logo.image_id, country, developed.name, developed.cover.image_id, developed.first_release_date, published.name, published.cover.image_id, published.first_release_date")
                .map(data -> new UnifiedCompanyProfileDto("igdb-" + id, first(text(data, "name"), "Unknown"), text(data, "description"),
                        igdbImage(text(data, "logo", "image_id")), text(data, "country"),
                        new Portfolio(items(data.path("developed")).stream().map(ProfileMapping::igdbMedia).toList(),
                                items(data.path("published")).stream().map(ProfileMapping::igdbMedia).toList(),
                                List.of(), List.of(), List.of(), List.of(), List.of())));
    }

    private Optional<UnifiedCompanyProfileDto> anilist(int id) {
        String query = "query ($id: Int) { Studio(id: $id) { id name isAnimationStudio "
                + "media(isMain: true, sort: POPULARITY_DESC, perPage: 50) { edges { node { " + ANILIST_MEDIA + " } } } } }";
        return providers.anilist("studio-" + id, query, Map.of("id", id), false).map(n -> n.path("Studio"))
                .filter(n -> !n.isNull() && !n.isMissingNode()).map(data -> {
                    List<ProfileMediaDto> animation = new ArrayList<>(), anime = new ArrayList<>(), manga = new ArrayList<>();
                    for (JsonNode edge : items(data.path("media").path("edges"))) {
                        JsonNode node = edge.path("node");
                        if (node.isNull() || node.isMissingNode()) continue;
                        ProfileMediaDto media = anilistMedia(node);
                        if (media.mediaType().equals("MANGA")) manga.add(media);
                        else if (data.path("isAnimationStudio").asBoolean()) animation.add(media);
                        else anime.add(media);
                    }
                    return new UnifiedCompanyProfileDto("anilist-" + id, first(text(data, "name"), "Unknown"), null, null, "JP",
                            new Portfolio(List.of(), List.of(), animation, anime, manga, List.of(), List.of()));
                });
    }

    private Optional<UnifiedCompanyProfileDto> tmdb(int id, boolean network) {
        return providers.tmdb((network ? "/network/" : "/company/") + id, Map.of()).map(data -> {
            Map<String, ?> params = Map.of(network ? "with_networks" : "with_companies", id, "sort_by", "popularity.desc");
            List<JsonNode> works = new ArrayList<>();
            if (!network) works.addAll(items(providers.tmdb("/discover/movie", params).orElse(MissingNode.getInstance()).path("results")));
            List<JsonNode> tv = items(providers.tmdb("/discover/tv", params).orElse(MissingNode.getInstance()).path("results"));
            // Preserve the endpoint's media type even if a TV result has no name/date.
            List<ProfileMediaDto> portfolio;
            if (network) portfolio = tv.stream().map(n -> tmdbMedia(n, true)).toList();
            else {
                record Work(JsonNode node, boolean tv) {}
                List<Work> combined = new ArrayList<>();
                works.forEach(n -> combined.add(new Work(n, false)));
                tv.forEach(n -> combined.add(new Work(n, true)));
                combined.sort(Comparator.comparingDouble((Work w) -> w.node().path("popularity").asDouble()).reversed());
                portfolio = combined.stream().map(w -> tmdbMedia(w.node(), w.tv())).toList();
            }
            return new UnifiedCompanyProfileDto((network ? "tmdbnet-" : "tmdb-") + id, first(text(data, "name"), "Unknown"),
                    network ? null : text(data, "description"), tmdbImage(text(data, "logo_path")), text(data, "origin_country"),
                    new Portfolio(List.of(), List.of(), List.of(), List.of(), List.of(), network ? List.of() : portfolio,
                            network ? portfolio : List.of()));
        });
    }
}
