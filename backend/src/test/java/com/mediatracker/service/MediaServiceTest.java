package com.mediatracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.client.*;
import com.mediatracker.model.dto.*;
import com.mediatracker.repository.MediaStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MediaServiceTest {
    private final TmdbClient tmdb = mock(TmdbClient.class);
    private final MangaDexClient manga = mock(MangaDexClient.class);
    private final AnilistClient anilist = mock(AnilistClient.class);
    private final AnimeThemesClient themes = mock(AnimeThemesClient.class);
    private final RawgClient rawg = mock(RawgClient.class);
    private final IgdbClient igdb = mock(IgdbClient.class);
    private final MediaStatsRepository stats = mock(MediaStatsRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private MediaService service;

    @BeforeEach
    void setUp() {
        service = new MediaService(tmdb, igdb, rawg, manga, anilist, themes, mock(JikanClient.class),
                new AnimeCanonService(), stats, mapper);
    }

    @Test
    void adjustsJsonNodeSeasonsAndKeepsProviderScore() throws Exception {
        MediaItemDto show = show();
        show.setGlobalScore(88);
        show.setSeasons(mapper.readTree("[{\"season_number\":4,\"episode_count\":28},{\"season_number\":0,\"episode_count\":37}]"));
        when(tmdb.getDetails(1429, "tv")).thenReturn(Optional.of(show));
        MediaItemDto result = service.getMediaDetails("tmdb-tv-1429").orElseThrow();
        var seasons = mapper.valueToTree(result.getSeasons());
        assertThat(seasons.get(0).path("episode_count").asInt()).isEqualTo(30);
        assertThat(seasons.get(1).path("episode_count").asInt()).isEqualTo(35);
        assertThat(result.getGlobalScore()).isEqualTo(88);
        verifyNoInteractions(stats, themes, anilist);
    }

    @Test
    void canonicalizesLegacyMangaAndRawgIds() {
        MediaItemDto md = new MediaItemDto();
        md.setId("mangadex-manga-abc");
        md.setType("manga");
        when(manga.getMangaByMalId(12)).thenReturn(Optional.of(md));
        when(manga.getMangaByAniListId(13)).thenReturn(Optional.of(md));
        assertThat(service.getMediaDetails("jikan-manga-12").orElseThrow().getId()).isEqualTo(md.getId());
        assertThat(service.getMediaDetails("anilist-manga-13").orElseThrow().getId()).isEqualTo(md.getId());
        MediaItemDto game = new MediaItemDto();
        game.setId("igdb-game-42");
        game.setTitle("Game");
        when(rawg.resolveRAWGToIGDB(7, igdb)).thenReturn(Optional.of(42));
        when(igdb.getGameDetails(42)).thenReturn(Optional.of(game));
        assertThat(service.getMediaDetails("rawg-game-7").orElseThrow().getId()).isEqualTo("igdb-game-42");
    }

    @Test
    void finaleCreditsUseOriginalSeasonZeroEpisode() {
        EpisodeDto regular = episode(10, 1);
        EpisodeDto special = episode(99, 36);
        when(tmdb.getSeasonEpisodes(1429, 4)).thenReturn(List.of(regular));
        when(tmdb.getSeasonEpisodes(1429, 0)).thenReturn(List.of(special));
        when(tmdb.getEpisodeCredits(1429, 0, 36)).thenReturn(Map.of("cast", List.of(), "crew", List.of()));
        service.getEpisodeCredits("tmdb-tv-1429", 4, 2);
        verify(tmdb).getEpisodeCredits(1429, 0, 36);
        assertThat(service.getSeasonEpisodes("tmdb-tv-1429", 0)).isEmpty();
    }

    @Test
    void resolvesAnimeAliasesByProviderTypeInsteadOfTreatingThemAsManga() throws Exception {
        MediaItemDto anime = new MediaItemDto();
        anime.setId("anilist-show-21");
        anime.setType("show");
        when(anilist.getDetails(21)).thenReturn(Optional.of(anime));
        when(anilist.getTmdbMapping(21)).thenReturn(Optional.of(1429));
        when(anilist.getAnilistRawDetails(21)).thenReturn(Optional.of(mapper.readTree("{\"format\":\"TV\"}")));
        when(tmdb.getDetails(1429, "tv")).thenReturn(Optional.of(show()));
        for (String alias : List.of("anilist-21", "anilist-anime-21", "anilist-show-21", "anilist-manga-21")) {
            assertThat(service.getMediaDetails(alias).orElseThrow().getId()).isEqualTo("tmdb-tv-1429");
        }
        verifyNoInteractions(manga);
    }

    @Test
    void unmappedAnimeRetainsSupportedCanonicalId() {
        MediaItemDto anime = new MediaItemDto();
        anime.setId("anilist-show-22");
        anime.setType("show");
        when(anilist.getDetails(22)).thenReturn(Optional.of(anime));
        assertThat(service.getMediaDetails("anilist-anime-22").orElseThrow().getId()).isEqualTo("anilist-show-22");
        assertThat(service.getMediaDetails("anilist-show-22").orElseThrow().getId()).isEqualTo("anilist-show-22");
        verifyNoInteractions(manga);
    }

    @Test
    void featureLengthSpecialUsesMovieMapping() throws Exception {
        MediaItemDto anime = new MediaItemDto();
        anime.setType("show");
        when(anilist.getDetails(23)).thenReturn(Optional.of(anime));
        when(anilist.getTmdbMapping(23)).thenReturn(Optional.of(99));
        when(anilist.getAnilistRawDetails(23)).thenReturn(Optional.of(mapper.readTree(
                "{\"format\":\"SPECIAL\",\"episodes\":1,\"duration\":60}")));
        MediaItemDto movie = new MediaItemDto();
        movie.setId("tmdb-movie-99");
        movie.setType("movie");
        when(tmdb.getDetails(99, "movie")).thenReturn(Optional.of(movie));
        assertThat(service.getMediaDetails("anilist-anime-23").orElseThrow().getId()).isEqualTo("tmdb-movie-99");
    }

    @Test
    void selectsSeasonThemesFromGroups() {
        MediaItemDto show = show();
        show.setOriginalLanguage("ja");
        show.setGenres(List.of("Animation"));
        AnimeThemeDto all = new AnimeThemeDto();
        all.setGroups(List.of(new AnimeThemeDto.AnimeThemeGroup("Season 2", 2, 2, List.of("Opening 2"), List.of("Ending 2"))));
        when(tmdb.getDetails(1429, "tv")).thenReturn(Optional.of(show));
        when(themes.fetchThemes("Show", null, false)).thenReturn(Optional.of(all));
        assertThat(service.getSeasonThemes("tmdb-tv-1429", 2).getOpenings()).containsExactly("Opening 2");
    }

    @Test
    void rejectsUnsupportedChapterAndSeasonIdentifiers() {
        assertThat(service.getSeasonEpisodes("tmdb-movie-1", 1)).isEmpty();
        assertThat(service.getSeasonEpisodes("tmdb-tv-1", -1)).isEmpty();
        assertThatThrownBy(() -> service.getChapterFeed("anilist-manga-1", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getChapterMetadata("mangadex-manga-not-a-uuid")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(tmdb, manga);
    }

    private MediaItemDto show() {
        MediaItemDto dto = new MediaItemDto();
        dto.setId("tmdb-tv-1429");
        dto.setTitle("Show");
        dto.setType("show");
        return dto;
    }

    private EpisodeDto episode(int id, int number) {
        EpisodeDto dto = new EpisodeDto();
        dto.setId(id);
        dto.setEpisodeNumber(number);
        return dto;
    }
}
