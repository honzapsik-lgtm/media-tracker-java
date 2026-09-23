package com.mediatracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.client.*;
import com.mediatracker.model.dto.*;
import com.mediatracker.model.entity.MediaStatsEntity;
import com.mediatracker.repository.MediaStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
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

                        // Anime themes
                        Optional<AnimeThemeDto> themes = animeThemesClient.fetchThemes(dto.getTitle(), dto.getOriginalTitle(), isMovie);
                        if (themes.isEmpty()) {
                            themes = jikanClient.searchAnimeThemes(dto.getTitle(), dto.getOriginalTitle(), isMovie);
                        }
                        themes.ifPresent(dto::setThemeData);

                        // If TV show, adjust seasons and canon movies
                        if (!isMovie) {
                            if (dto.getSeasons() instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> seasonsList = (List<Map<String, Object>>) dto.getSeasons();
                                dto.setSeasons(animeCanonService.getAdjustedSeasons(String.valueOf(id), seasonsList));
                            }
                            List<AnimeCanonService.CanonMovieItem> canonMovies = animeCanonService.getCanonMoviesForShow(String.valueOf(id));
                            if (!canonMovies.isEmpty()) {
                                dto.setCanonMovies(new ArrayList<>(canonMovies));
                            }

                            // Related manga check
                            anilistClient.searchManga(dto.getTitle()).stream().findFirst().ifPresent(m -> {
                                dto.setRelatedManga(Map.of("id", m.getId(), "title", m.getTitle(), "image", m.getImage() != null ? m.getImage() : ""));
                            });
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
            if (parts.length >= 3 && parts[1].equalsIgnoreCase("manga")) {
                try {
                    int id = Integer.parseInt(parts[2]);
                    itemOpt = anilistClient.getDetails(id);
                } catch (NumberFormatException e) {
                    log.warn("Invalid AniList ID in slug: {}", slug);
                }
            }
        }

        // Attach community stats if present
        if (itemOpt.isPresent()) {
            MediaItemDto dto = itemOpt.get();
            Optional<MediaStatsEntity> stats = mediaStatsRepository.findById(dto.getId());
            if (stats.isPresent()) {
                MediaStatsEntity s = stats.get();
                if (s.getCommunityAverage() != null) {
                    dto.setGlobalScore(s.getCommunityAverage().intValue());
                }
            }
        }

        return itemOpt;
    }

    public List<EpisodeDto> getSeasonEpisodes(String slug, int seasonNumber) {
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

    private Integer parseYear(String dateStr) {
        if (dateStr == null || dateStr.length() < 4) return null;
        try {
            return Integer.parseInt(dateStr.substring(0, 4));
        } catch (Exception e) {
            return null;
        }
    }
}
