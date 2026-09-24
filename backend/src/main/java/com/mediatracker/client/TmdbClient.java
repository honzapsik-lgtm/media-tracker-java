package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.dto.DiscoverItemDto;
import com.mediatracker.model.dto.EpisodeDto;
import com.mediatracker.model.dto.MediaCreditDto;
import com.mediatracker.model.dto.MediaItemDto;
import com.mediatracker.service.CacheService;
import com.mediatracker.service.CreditsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final int CACHE_TTL_SECONDS = 7 * 24 * 3600; // 7 days

    private final RestClient restClient;
    private final CacheService cacheService;
    private final CreditsParser creditsParser;
    private final ObjectMapper objectMapper;

    @Value("${app.providers.tmdb.api-key:}")
    private String apiKey;

    public TmdbClient(RestClient restClient, CacheService cacheService, CreditsParser creditsParser, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.cacheService = cacheService;
        this.creditsParser = creditsParser;
        this.objectMapper = objectMapper;
    }

    public Optional<MediaItemDto> getDetails(int id, String type) {
        String cacheKey = "tmdb-details-v2-" + type + "-" + id;
        Optional<MediaItemDto> cached = cacheService.get(cacheKey, MediaItemDto.class);
        if (cached.isPresent()) {
            return cached;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TMDB API Key missing");
            return Optional.empty();
        }

        try {
            String appendParams = "tv".equals(type)
                    ? "credits,aggregate_credits,videos,release_dates,watch/providers"
                    : "credits,videos,release_dates,watch/providers";

            String uri = String.format("%s/%s/%d?api_key=%s&language=en-US&append_to_response=%s",
                    BASE_URL, type, id, apiKey, appendParams);

            JsonNode data = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

            if (data == null || data.isNull()) {
                return Optional.empty();
            }

            MediaItemDto dto = new MediaItemDto();
            dto.setId("tmdb-" + type + "-" + id);
            dto.setTitle(data.hasNonNull("title") ? data.get("title").asText() : data.path("name").asText("Untitled"));
            dto.setOriginalTitle(data.hasNonNull("original_title") ? data.get("original_title").asText() : data.path("original_name").asText(null));
            dto.setOriginalLanguage(data.path("original_language").asText(null));
            dto.setType("tv".equals(type) ? "show" : "movie");

            String posterPath = data.path("poster_path").asText(null);
            dto.setImage(posterPath != null ? "https://image.tmdb.org/t/p/w500" + posterPath : null);

            String backdropPath = data.path("backdrop_path").asText(null);
            dto.setBackdrop(backdropPath != null ? "https://image.tmdb.org/t/p/original" + backdropPath : null);

            dto.setDescription(data.path("overview").asText(""));
            dto.setReleaseDate(data.hasNonNull("release_date") ? data.get("release_date").asText() : data.path("first_air_date").asText(null));

            double voteAvg = data.path("vote_average").asDouble(0.0);
            dto.setGlobalScore((int) Math.round(voteAvg * 10));

            int runtime = data.path("runtime").asInt(0);
            if (runtime == 0 && data.has("episode_run_time") && data.get("episode_run_time").isArray() && !data.get("episode_run_time").isEmpty()) {
                runtime = data.get("episode_run_time").get(0).asInt(0);
            }
            dto.setRuntime(runtime > 0 ? runtime : null);

            // Genres
            List<String> genres = new ArrayList<>();
            JsonNode genresNode = data.path("genres");
            if (genresNode.isArray()) {
                for (JsonNode g : genresNode) {
                    if (g.hasNonNull("name")) genres.add(g.get("name").asText());
                }
            }
            dto.setGenres(genres);

            // Trailer
            JsonNode videos = data.path("videos").path("results");
            if (videos.isArray()) {
                for (JsonNode vid : videos) {
                    if ("YouTube".equalsIgnoreCase(vid.path("site").asText()) && "Trailer".equalsIgnoreCase(vid.path("type").asText())) {
                        dto.setTrailerUrl("https://www.youtube.com/embed/" + vid.path("key").asText());
                        break;
                    }
                }
            }

            // Cast & Credits
            JsonNode castNode = "tv".equals(type) && data.path("aggregate_credits").path("cast").isArray() && !data.path("aggregate_credits").path("cast").isEmpty()
                    ? data.path("aggregate_credits").path("cast")
                    : data.path("credits").path("cast");

            List<MediaCreditDto> cast = new ArrayList<>();
            if (castNode.isArray()) {
                for (JsonNode actor : castNode) {
                    String charName = actor.path("character").asText("Unknown Role");
                    if (actor.has("roles") && actor.get("roles").isArray() && !actor.get("roles").isEmpty()) {
                        charName = actor.get("roles").get(0).path("character").asText(charName);
                    }
                    String profilePath = actor.path("profile_path").asText(null);
                    cast.add(new MediaCreditDto(
                            "tmdb-" + actor.path("id").asText(),
                            actor.path("name").asText("Unknown"),
                            charName,
                            "Actor",
                            profilePath != null ? "https://image.tmdb.org/t/p/w200" + profilePath : null
                    ));
                }
            }
            dto.setCast(cast);

            // Crew
            JsonNode crewNode = "tv".equals(type) && data.path("aggregate_credits").path("crew").isArray() && !data.path("aggregate_credits").path("crew").isEmpty()
                    ? data.path("aggregate_credits").path("crew")
                    : data.path("credits").path("crew");

            List<MediaCreditDto> credits = new ArrayList<>();
            if (crewNode.isArray()) {
                for (JsonNode crewMember : crewNode) {
                    String rawJob = crewMember.path("job").asText("");
                    if (rawJob.isBlank() && crewMember.has("jobs") && crewMember.get("jobs").isArray() && !crewMember.get("jobs").isEmpty()) {
                        rawJob = crewMember.get("jobs").get(0).path("job").asText("");
                    }
                    String role = creditsParser.normalizeTMDbRole(rawJob);
                    String profile = crewMember.path("profile_path").asText(null);
                    credits.add(new MediaCreditDto(
                            "tmdb-" + crewMember.path("id").asText(),
                            crewMember.path("name").asText("Unknown"),
                            null,
                            role,
                            profile != null ? "https://image.tmdb.org/t/p/w200" + profile : null
                    ));
                }
            }
            dto.setCredits(credits);

            List<Object> studios = new ArrayList<>();
            for (String field : List.of("production_companies", "networks")) {
                for (JsonNode studio : data.path(field)) {
                    studios.add(java.util.Map.of("id", ("networks".equals(field) ? "tmdbnet-" : "tmdb-")
                            + studio.path("id").asText(), "name", studio.path("name").asText()));
                }
            }
            dto.setStudios(studios);

            // Seasons summary for TV
            if (data.has("seasons")) {
                dto.setSeasons(data.get("seasons"));
            }

            // Watch providers (US)
            JsonNode usProviders = data.path("watch/providers").path("results").path("US");
            if (!usProviders.isMissingNode()) {
                dto.setWatchData(usProviders);
            }

            cacheService.put(cacheKey, "tmdb", dto, CACHE_TTL_SECONDS);
            return Optional.of(dto);
        } catch (Exception e) {
            log.error("Failed to fetch TMDb details for {}-{}", type, id, e);
            return Optional.empty();
        }
    }

    public List<EpisodeDto> getSeasonEpisodes(int tvId, int seasonNumber) {
        String cacheKey = String.format("tmdb-episodes-%d-%d", tvId, seasonNumber);
        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, EpisodeDto.class));
        }

        if (apiKey == null || apiKey.isBlank()) return List.of();

        try {
            String uri = String.format("%s/tv/%d/season/%d?api_key=%s&language=en-US", BASE_URL, tvId, seasonNumber, apiKey);
            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data == null || !data.has("episodes") || !data.get("episodes").isArray()) {
                return List.of();
            }

            List<EpisodeDto> episodes = new ArrayList<>();
            for (JsonNode ep : data.get("episodes")) {
                EpisodeDto dto = new EpisodeDto();
                dto.setId(ep.path("id").asInt());
                dto.setName(ep.path("name").asText(""));
                dto.setEpisodeNumber(ep.path("episode_number").asInt());
                dto.setOverview(ep.path("overview").asText(""));
                String still = ep.path("still_path").asText(null);
                dto.setImage(still != null ? "https://image.tmdb.org/t/p/w400" + still : null);
                dto.setAirDate(ep.path("air_date").asText(null));
                dto.setRuntime(ep.path("runtime").asInt(0));
                double v = ep.path("vote_average").asDouble(0.0);
                dto.setGlobalScore((int) Math.round(v * 10));
                episodes.add(dto);
            }

            cacheService.put(cacheKey, "tmdb", episodes, CACHE_TTL_SECONDS);
            return episodes;
        } catch (Exception e) {
            log.warn("Failed to fetch TMDb season episodes for tvId={}, season={}: {}", tvId, seasonNumber, e.getMessage());
            return List.of();
        }
    }

    public java.util.Map<String, List<MediaCreditDto>> getEpisodeCredits(int tvId, int season, int episode) {
        if (apiKey == null || apiKey.isBlank()) return java.util.Map.of("cast", List.of(), "crew", List.of());
        String key = "tmdb-episode-credits-" + tvId + "-" + season + "-" + episode;
        JsonNode data = cacheService.get(key, JsonNode.class).orElseGet(() -> {
            JsonNode response = restClient.get().uri(String.format(
                    "%s/tv/%d/season/%d/episode/%d/credits?api_key=%s", BASE_URL, tvId, season, episode, apiKey))
                    .retrieve().body(JsonNode.class);
            if (response != null) cacheService.put(key, "tmdb", response, CACHE_TTL_SECONDS);
            return response;
        });
        List<MediaCreditDto> cast = new ArrayList<>();
        List<MediaCreditDto> crew = new ArrayList<>();
        if (data != null) {
            for (String field : List.of("cast", "guest_stars", "crew")) {
                for (JsonNode person : data.path(field)) {
                    boolean isCrew = "crew".equals(field);
                    String image = person.path("profile_path").asText(null);
                    MediaCreditDto credit = new MediaCreditDto("tmdb-" + person.path("id").asText(),
                            person.path("name").asText(), person.path("character").asText(null),
                            isCrew ? creditsParser.normalizeTMDbRole(person.path("job").asText()) : "Actor",
                            image == null ? null : "https://image.tmdb.org/t/p/w200" + image);
                    if (isCrew) crew.add(credit);
                    else if (cast.stream().noneMatch(c -> c.getId().equals(credit.getId()))) cast.add(credit);
                }
            }
        }
        return java.util.Map.of("cast", cast, "crew", crew);
    }

    public List<DiscoverItemDto> discover(String type, Integer genreId, String year, String sort, int page) {
        String tmdbType = "movie".equalsIgnoreCase(type) ? "movie" : "tv";
        String cacheKey = String.format("discover-%s-%s-%s-%s-%d", tmdbType, genreId != null ? genreId : "all", year != null ? year : "all", sort != null ? sort : "default", page);

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, DiscoverItemDto.class));
        }

        if (apiKey == null || apiKey.isBlank()) return List.of();

        try {
            StringBuilder sb = new StringBuilder(String.format("%s/discover/%s?api_key=%s&language=en-US&page=%d", BASE_URL, tmdbType, apiKey, page));
            if (genreId != null) sb.append("&with_genres=").append(genreId);
            if (year != null && !year.isBlank()) {
                sb.append("movie".equals(tmdbType) ? "&primary_release_year=" : "&first_air_date_year=").append(year);
            }
            if (sort != null && !sort.isBlank()) {
                String tmdbSort = "popular".equals(sort) ? "popularity.desc" : "top_rated".equals(sort) ? "vote_average.desc" : sort;
                sb.append("&sort_by=").append(tmdbSort);
            }

            JsonNode data = restClient.get().uri(sb.toString()).retrieve().body(JsonNode.class);
            if (data == null || !data.has("results") || !data.get("results").isArray()) {
                return List.of();
            }

            List<DiscoverItemDto> results = new ArrayList<>();
            for (JsonNode item : data.get("results")) {
                int id = item.path("id").asInt();
                String title = item.hasNonNull("title") ? item.get("title").asText() : item.path("name").asText("Untitled");
                String poster = item.path("poster_path").asText(null);
                String backdrop = item.path("backdrop_path").asText(null);
                String image = poster != null ? "https://image.tmdb.org/t/p/w500" + poster : backdrop != null ? "https://image.tmdb.org/t/p/w500" + backdrop : "";
                double v = item.path("vote_average").asDouble(0.0);
                String date = "movie".equals(tmdbType) ? item.path("release_date").asText(null) : item.path("first_air_date").asText(null);

                results.add(new DiscoverItemDto(
                        "tmdb-" + tmdbType + "-" + id,
                        title,
                        image,
                        "movie".equals(tmdbType) ? "movie" : "show",
                        (int) Math.round(v * 10),
                        date
                ));
            }

            cacheService.put(cacheKey, "tmdb", results, 6 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("TMDb discover request failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<DiscoverItemDto> getRecommendations(int tmdbId, String type) {
        if (apiKey == null || apiKey.isBlank()) return List.of();
        String tmdbType = "tv".equalsIgnoreCase(type) || "show".equalsIgnoreCase(type) ? "tv" : "movie";
        String cacheKey = "tmdb-rec-" + tmdbType + "-" + tmdbId;

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, DiscoverItemDto.class));
        }

        try {
            String uri = String.format("%s/%s/%d/recommendations?api_key=%s&language=en-US&page=1",
                    BASE_URL, tmdbType, tmdbId, apiKey);

            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data == null || !data.has("results") || !data.get("results").isArray()) {
                return List.of();
            }

            List<DiscoverItemDto> results = new ArrayList<>();
            for (JsonNode item : data.get("results")) {
                int id = item.path("id").asInt();
                String title = item.hasNonNull("title") ? item.get("title").asText() : item.path("name").asText("Untitled");
                String poster = item.path("poster_path").asText(null);
                String backdrop = item.path("backdrop_path").asText(null);
                String image = poster != null ? "https://image.tmdb.org/t/p/w500" + poster : backdrop != null ? "https://image.tmdb.org/t/p/w500" + backdrop : "";
                double v = item.path("vote_average").asDouble(0.0);
                String date = "movie".equals(tmdbType) ? item.path("release_date").asText(null) : item.path("first_air_date").asText(null);

                results.add(new DiscoverItemDto(
                        "tmdb-" + tmdbType + "-" + id,
                        title,
                        image,
                        "movie".equals(tmdbType) ? "movie" : "show",
                        (int) Math.round(v * 10),
                        date
                ));
            }

            cacheService.put(cacheKey, "tmdb", results, 24 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("TMDb recommendations request failed for {}-{}: {}", tmdbType, tmdbId, e.getMessage());
            return List.of();
        }
    }

    public List<MediaItemDto> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = "tmdb-search-" + normalizedQuery;

        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return objectMapper.convertValue(cached.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, MediaItemDto.class));
        }

        if (apiKey == null || apiKey.isBlank()) return List.of();

        try {
            String uri = String.format("%s/search/multi?api_key=%s&query=%s&include_adult=false&language=en-US&page=1",
                    BASE_URL, apiKey, java.net.URLEncoder.encode(normalizedQuery, java.nio.charset.StandardCharsets.UTF_8));

            JsonNode data = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (data == null || !data.has("results") || !data.get("results").isArray()) {
                return List.of();
            }

            List<MediaItemDto> results = new ArrayList<>();
            for (JsonNode item : data.get("results")) {
                String mediaType = item.path("media_type").asText("");
                if (!"movie".equalsIgnoreCase(mediaType) && !"tv".equalsIgnoreCase(mediaType)) {
                    continue;
                }

                int id = item.path("id").asInt();
                boolean isMovie = "movie".equalsIgnoreCase(mediaType);
                String title = item.hasNonNull("title") ? item.get("title").asText()
                        : item.path("name").asText("Untitled");
                String poster = item.path("poster_path").asText(null);
                String image = poster != null ? "https://image.tmdb.org/t/p/w500" + poster : null;
                String releaseDate = isMovie ? item.path("release_date").asText(null)
                        : item.path("first_air_date").asText(null);

                MediaItemDto dto = new MediaItemDto();
                dto.setId(isMovie ? "tmdb-movie-" + id : "tmdb-tv-" + id);
                dto.setTitle(title);
                dto.setType(isMovie ? "movie" : "show");
                dto.setImage(image);
                dto.setReleaseDate(releaseDate != null ? releaseDate : "N/A");
                dto.setOrigin("TMDB");
                results.add(dto);
            }

            cacheService.put(cacheKey, "tmdb", results, 24 * 3600);
            return results;
        } catch (Exception e) {
            log.warn("TMDb search error for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
