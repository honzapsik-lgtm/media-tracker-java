package com.mediatracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.client.*;
import com.mediatracker.model.dto.*;
import com.mediatracker.model.entity.MediaStatsEntity;
import com.mediatracker.repository.MediaStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final TmdbClient tmdbClient;
    private final IgdbClient igdbClient;
    private final RawgClient rawgClient;
    private final MangaDexClient mangaDexClient;
    private final AnilistClient anilistClient;
    private final AnimeThemesClient animeThemesClient;
    private final JikanClient jikanClient;
    private final AnimeCanonService animeCanonService;
    private final MediaStatsRepository mediaStatsRepository;
    private final ObjectMapper objectMapper;

    public MediaService(TmdbClient tmdbClient,
                        IgdbClient igdbClient,
                        RawgClient rawgClient,
                        MangaDexClient mangaDexClient,
                        AnilistClient anilistClient,
                        AnimeThemesClient animeThemesClient,
                        JikanClient jikanClient,
                        AnimeCanonService animeCanonService,
                        MediaStatsRepository mediaStatsRepository,
                        ObjectMapper objectMapper) {
        this.tmdbClient = tmdbClient;
        this.igdbClient = igdbClient;
        this.rawgClient = rawgClient;
        this.mangaDexClient = mangaDexClient;
        this.anilistClient = anilistClient;
        this.animeThemesClient = animeThemesClient;
        this.jikanClient = jikanClient;
        this.animeCanonService = animeCanonService;
        this.mediaStatsRepository = mediaStatsRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<MediaItemDto> getMediaDetails(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        String[] parts = slug.split("-");

        Optional<MediaItemDto> itemOpt = Optional.empty();

        if (parts[0].equalsIgnoreCase("tmdb")) {
            if (parts.length >= 3) {
                String tmdbType = parts[1].toLowerCase();
                try {
                    int id = Integer.parseInt(parts[2]);
                    itemOpt = tmdbClient.getDetails(id, "tv".equals(tmdbType) || "show".equals(tmdbType) ? "tv" : "movie");

                    if (itemOpt.isPresent()) {
                        MediaItemDto dto = itemOpt.get();
                        boolean isMovie = "movie".equalsIgnoreCase(dto.getType());

                        boolean isAnime = "ja".equals(dto.getOriginalLanguage()) && dto.getGenres().contains("Animation");
                        if (isAnime) {
                            Optional<AnimeThemeDto> themes = animeThemesClient.fetchThemes(dto.getTitle(), dto.getOriginalTitle(), isMovie);
                            if (themes.isEmpty()) themes = jikanClient.searchAnimeThemes(dto.getTitle(), dto.getOriginalTitle(), isMovie);
                            themes.ifPresent(dto::setThemeData);
                            anilistClient.searchManga(dto.getTitle()).stream().findFirst().ifPresent(m ->
                                    dto.setRelatedManga(Map.of("id", m.getId(), "title", m.getTitle(), "image", m.getImage() != null ? m.getImage() : "")));
                        }

                        // If TV show, adjust seasons and canon movies
                        if (!isMovie) {
                            if (dto.getSeasons() != null) {
                                List<Map<String, Object>> seasonsList = objectMapper.convertValue(dto.getSeasons(),
                                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                                dto.setSeasons(animeCanonService.getAdjustedSeasons(String.valueOf(id), seasonsList));
                            }
                            List<AnimeCanonService.CanonMovieItem> canonMovies = animeCanonService.getCanonMoviesForShow(String.valueOf(id));
                            if (!canonMovies.isEmpty()) {
                                dto.setCanonMovies(new ArrayList<>(canonMovies));
                            }

                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid TMDb ID in slug: {}", slug);
                }
            }
        } else if (parts[0].equalsIgnoreCase("igdb")) {
            if (parts.length >= 3 && parts[1].equalsIgnoreCase("game")) {
                try {
                    int id = Integer.parseInt(parts[2]);
                    itemOpt = igdbClient.getGameDetails(id);
                    if (itemOpt.isPresent()) {
                        MediaItemDto dto = itemOpt.get();
                        Integer year = parseYear(dto.getReleaseDate());
                        List<MediaCreditDto> crew = rawgClient.getGameCrew(dto.getTitle(), year);
                        if (!crew.isEmpty()) {
                            dto.setCredits(crew);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid IGDB ID in slug: {}", slug);
                }
            }
        } else if (parts[0].equalsIgnoreCase("rawg")) {
            if (parts.length >= 3 && parts[1].equalsIgnoreCase("game")) {
                try {
                    int rawgId = Integer.parseInt(parts[2]);
                    Optional<Integer> igdbId = rawgClient.resolveRAWGToIGDB(rawgId, igdbClient);
                    if (igdbId.isPresent()) {
                        return getMediaDetails("igdb-game-" + igdbId.get());
                    }
                    itemOpt = rawgClient.getGameDetails(rawgId);
                    itemOpt.ifPresent(dto -> dto.setCredits(rawgClient.getGameCrew(dto.getTitle(), parseYear(dto.getReleaseDate()))));
                } catch (NumberFormatException e) {
                    log.warn("Invalid RAWG ID in slug: {}", slug);
                }
            }
        } else if (parts[0].equalsIgnoreCase("mangadex")) {
            if (parts.length >= 3 && parts[1].equalsIgnoreCase("manga")) {
                String mdId = slug.substring("mangadex-manga-".length());
                itemOpt = mangaDexClient.getMangaDetails(mdId);
            }
        } else if (parts[0].equalsIgnoreCase("anilist")) {
            if (parts.length == 2 || (parts.length == 3 && List.of("manga", "anime", "show", "movie").contains(parts[1]))) {
                try {
                    int id = Integer.parseInt(parts[parts.length - 1]);
                    itemOpt = resolveAnilist(id);
                } catch (NumberFormatException e) {
                    log.warn("Invalid AniList ID in slug: {}", slug);
                }
            }
        } else if (parts.length == 3 && "jikan".equalsIgnoreCase(parts[0]) && "manga".equalsIgnoreCase(parts[1])) {
            try {
                itemOpt = mangaDexClient.getMangaByMalId(Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        itemOpt.ifPresent(this::enrichManga);
        return itemOpt;
    }

    private void enrichManga(MediaItemDto dto) {
        if (!"manga".equals(dto.getType()) || dto.getAnilistId() == null) return;
        anilistClient.getAnilistRawDetails(dto.getAnilistId()).ifPresent(data -> {
            if (data.isNull()) return;
            dto.setBackdrop(data.path("bannerImage").asText(dto.getBackdrop()));
            if (data.path("averageScore").asInt() > 0) dto.setGlobalScore(data.path("averageScore").asInt());
            if (dto.getChapters() == null && data.path("chapters").asInt() > 0) dto.setChapters(data.path("chapters").asInt());
            if (dto.getVolumes() == null && data.path("volumes").asInt() > 0) dto.setVolumes(data.path("volumes").asInt());
            if ("youtube".equals(data.path("trailer").path("site").asText())) {
                dto.setTrailerUrl("https://www.youtube.com/embed/" + data.path("trailer").path("id").asText());
            }
            List<Object> links = new ArrayList<>();
            data.path("externalLinks").forEach(link -> links.add(objectMapper.convertValue(link, Map.class)));
            dto.setExternalLinks(links);
            List<Object> related = new ArrayList<>();
            for (var edge : data.path("relations").path("edges")) {
                var node = edge.path("node");
                if (!List.of("MANGA", "NOVEL", "ONE_SHOT").contains(node.path("format").asText())) continue;
                String title = node.path("title").path("english").asText(node.path("title").path("romaji").asText("Unknown"));
                related.add(Map.of("id", "anilist-manga-" + node.path("id").asInt(), "title", title,
                        "type", "MANGA", "relationLabel", edge.path("relationType").asText("Related").replace('_', ' ')));
            }
            dto.setRelatedMedia(related);
        });
    }

    private Optional<MediaItemDto> resolveAnilist(int id) {
        Optional<MediaItemDto> item = anilistClient.getDetails(id);
        if (item.isEmpty() || "manga".equals(item.get().getType())) {
            Optional<MediaItemDto> manga = mangaDexClient.getMangaByAniListId(id);
            if (manga.isPresent()) item = manga;
        } else {
            Optional<Integer> tmdbId = anilistClient.getTmdbMapping(id);
            if (tmdbId.isPresent()) {
                boolean feature = anilistClient.getAnilistRawDetails(id).map(data -> {
                    String format = data.path("format").asText();
                    return "MOVIE".equals(format) || (List.of("ONA", "OVA", "SPECIAL").contains(format)
                            && data.path("episodes").asInt() == 1 && data.path("duration").asInt() >= 45);
                }).orElse(false);
                Optional<MediaItemDto> mapped = getMediaDetails("tmdb-" + (feature ? "movie-" : "tv-") + tmdbId.get());
                if (mapped.isPresent()) return mapped;
            }
        }
        item.ifPresent(dto -> dto.setAnilistId(id));
        return item;
    }

    public List<EpisodeDto> getSeasonEpisodes(String slug, int seasonNumber) {
        if (!slug.matches("tmdb-tv-\\d+") || seasonNumber < 0) return List.of();
        String[] parts = slug.split("-");
        if (!parts[0].equalsIgnoreCase("tmdb") || parts.length < 3) return List.of();

        try {
            int tvId = Integer.parseInt(parts[2]);
            List<EpisodeDto> rawEpisodes = new ArrayList<>(tmdbClient.getSeasonEpisodes(tvId, seasonNumber));

            // 1. Season 0 filtering (OVAs vs Integrated Specials)
            if (seasonNumber == 0) {
                List<Integer> excluded = animeCanonService.getExcludedSeason0EpisodeNumbers(String.valueOf(tvId));
                if (!excluded.isEmpty()) {
                    rawEpisodes.removeIf(ep -> excluded.contains(ep.getEpisodeNumber()));
                }
            }

            // 2. Append canon finale specials to regular season (e.g. AoT Season 4 finale specials)
            List<Integer> canonSpecials = animeCanonService.getCanonFinaleEpisodeNumbers(String.valueOf(tvId), seasonNumber);
            if (!canonSpecials.isEmpty()) {
                List<EpisodeDto> s0Episodes = tmdbClient.getSeasonEpisodes(tvId, 0);
                for (EpisodeDto s0Ep : s0Episodes) {
                    if (canonSpecials.contains(s0Ep.getEpisodeNumber())) {
                        EpisodeDto special = new EpisodeDto();
                        special.setId(s0Ep.getId());
                        special.setName(s0Ep.getName());
                        special.setEpisodeNumber(rawEpisodes.size() + 1);
                        special.setOverview(s0Ep.getOverview());
                        special.setImage(s0Ep.getImage());
                        special.setAirDate(s0Ep.getAirDate());
                        special.setRuntime(s0Ep.getRuntime());
                        special.setGlobalScore(s0Ep.getGlobalScore());
                        special.setFinaleSpecial(true);
                        rawEpisodes.add(special);
                    }
                }
            }

            return rawEpisodes;
        } catch (NumberFormatException e) {
            log.warn("Invalid tvId in slug {}: {}", slug, e.getMessage());
            return List.of();
        }
    }

    public Object getChapterFeed(String slug, int offset) {
        return mangaDexClient.getChapterFeed(mangaDexId(slug), offset);
    }

    public Object getChapterMetadata(String slug) {
        return mangaDexClient.getChapterMetadata(mangaDexId(slug));
    }

    private String mangaDexId(String slug) {
        if (!slug.startsWith("mangadex-manga-")) throw new IllegalArgumentException("Expected a MangaDex manga slug");
        String id = slug.substring("mangadex-manga-".length());
        UUID.fromString(id);
        return id;
    }

    public Map<String, List<MediaCreditDto>> getEpisodeCredits(String slug, int season, int episode) {
        if (!slug.matches("tmdb-tv-\\d+") || season < 0 || episode < 1) return Map.of("cast", List.of(), "crew", List.of());
        int tvId = Integer.parseInt(slug.substring("tmdb-tv-".length()));
        Optional<EpisodeDto> target = getSeasonEpisodes(slug, season).stream()
                .filter(ep -> Objects.equals(ep.getEpisodeNumber(), episode)).findFirst();
        if (target.isEmpty()) return Map.of("cast", List.of(), "crew", List.of());
        if (Boolean.TRUE.equals(target.get().getIsFinaleSpecial())) {
            Optional<EpisodeDto> original = tmdbClient.getSeasonEpisodes(tvId, 0).stream()
                    .filter(ep -> Objects.equals(ep.getId(), target.get().getId())).findFirst();
            if (original.isEmpty()) return Map.of("cast", List.of(), "crew", List.of());
            return tmdbClient.getEpisodeCredits(tvId, 0, original.get().getEpisodeNumber());
        }
        return tmdbClient.getEpisodeCredits(tvId, season, episode);
    }

    public AnimeThemeDto getSeasonThemes(String slug, int season) {
        Optional<MediaItemDto> media = getMediaDetails(slug);
        if (media.isEmpty() || media.get().getThemeData() == null) return new AnimeThemeDto();
        AnimeThemeDto themes = objectMapper.convertValue(media.get().getThemeData(), AnimeThemeDto.class);
        for (AnimeThemeDto.AnimeThemeGroup group : themes.getGroups()) {
            String name = group.getSeasonName().toLowerCase(Locale.ROOT);
            if (Objects.equals(group.getSeasonNumber(), season)
                    || (season == 0 && (name.contains("special") || name.contains("ova")))) {
                return new AnimeThemeDto(group.getOpenings(), group.getEndings());
            }
        }
        String label = "";
        for (var summary : objectMapper.valueToTree(media.get().getSeasons())) {
            if (summary.path("season_number").asInt(-1) == season) label = summary.path("name").asText("").toLowerCase(Locale.ROOT);
        }
        if (!label.isBlank()) {
            for (var group : themes.getGroups()) {
                String name = group.getSeasonName().toLowerCase(Locale.ROOT);
                if (name.contains(label) || label.contains(name)) return new AnimeThemeDto(group.getOpenings(), group.getEndings());
            }
        }
        return themes;
    }

    private Integer parseYear(String dateStr) {
        if (dateStr == null || dateStr.length() < 4) return null;
        try {
            return Integer.parseInt(dateStr.substring(0, 4));
        } catch (Exception e) {
            return null;
        }
    }
}
