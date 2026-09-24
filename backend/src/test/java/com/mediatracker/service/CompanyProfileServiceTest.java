package com.mediatracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.client.ProfileProviderClient;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompanyProfileServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProfileProviderClient providers = mock(ProfileProviderClient.class);
    private final CompanyProfileService service = new CompanyProfileService(providers);
    private Optional<JsonNode> json(String value) throws Exception { return Optional.of(mapper.readTree(value)); }

    @Test
    void tmdbCompanyCombinesAndSortsMoviesAndTv() throws Exception {
        when(providers.tmdb("/company/1", Map.of())).thenReturn(json("""
            {"name":"Company","description":"Bio","logo_path":"/logo.jpg","origin_country":"US"}
            """));
        when(providers.tmdb(eq("/discover/movie"), anyMap())).thenReturn(json("""
            {"results":[{"id":2,"title":"Movie","popularity":2,"release_date":"2000-01-01"}]}
            """));
        when(providers.tmdb(eq("/discover/tv"), anyMap())).thenReturn(json("""
            {"results":[{"id":3,"name":"Show","popularity":9}]}
            """));
        var profile = service.getProfile("TMDB-company-1").orElseThrow();
        assertThat(profile.portfolio().producedFilmTv()).extracting(m -> m.mediaId()).containsExactly("tmdb-tv-3", "tmdb-movie-2");
        assertThat(profile.description()).isEqualTo("Bio");
        JsonNode portfolio = mapper.valueToTree(profile.portfolio());
        assertThat(portfolio.size()).isEqualTo(7);
        assertThat(portfolio.path("developedGames").isArray()).isTrue();
        assertThat(portfolio.path("broadcastedOn").isEmpty()).isTrue();
        verify(providers).tmdb("/discover/tv", Map.of("with_companies", 1, "sort_by", "popularity.desc"));
    }

    @Test
    void networkUsesSeparateFilterAndBroadcastBucket() throws Exception {
        when(providers.tmdb("/network/4", Map.of())).thenReturn(json("{\"name\":\"Network\"}"));
        when(providers.tmdb("/discover/tv", Map.of("with_networks", 4, "sort_by", "popularity.desc")))
                .thenReturn(json("{\"results\":[{\"id\":5,\"name\":\"Show\"}]}"));
        var profile = service.getProfile("tmdbnet-4").orElseThrow();
        assertThat(profile.id()).isEqualTo("tmdbnet-4");
        assertThat(profile.portfolio().broadcastedOn()).hasSize(1);
        assertThat(profile.portfolio().producedFilmTv()).isEmpty();
        verify(providers, never()).tmdb(eq("/discover/movie"), anyMap());
    }

    @Test
    void anilistSeparatesAnimationAndManga() throws Exception {
        when(providers.anilist(eq("studio-5"), contains("isMain: true"), anyMap(), eq(false))).thenReturn(json("""
            {"Studio":{"name":"Studio","isAnimationStudio":true,"media":{"edges":[
              {"node":{"id":1,"type":"ANIME","title":{"romaji":"Anime"}}},
              {"node":{"id":2,"type":"MANGA","title":{"english":"Manga"}}},{"node":null}]}}}
            """));
        var profile = service.getProfile("anilist-5").orElseThrow();
        assertThat(profile.country()).isEqualTo("JP");
        assertThat(profile.portfolio().animationStudioFor()).hasSize(1);
        assertThat(profile.portfolio().animationStudioFor().getFirst().mediaId()).isEqualTo("anilist-show-1");
        assertThat(profile.portfolio().publishedManga()).hasSize(1);
        assertThat(profile.portfolio().publishedManga().getFirst().mediaId()).isEqualTo("anilist-manga-2");
        assertThat(profile.portfolio().producedAnime()).isEmpty();
    }

    @Test
    void anilistNonAnimationStudioProducesAnime() throws Exception {
        when(providers.anilist(anyString(), anyString(), anyMap(), anyBoolean())).thenReturn(json("""
            {"Studio":{"name":"Producer","isAnimationStudio":false,"media":{"edges":[{"node":{"id":1,"type":"ANIME"}}]}}}
            """));
        var profile = service.getProfile("anilist-5").orElseThrow();
        assertThat(profile.portfolio().producedAnime()).hasSize(1);
        assertThat(profile.portfolio().animationStudioFor()).isEmpty();
    }

    @Test
    void igdbKeepsDevelopedAndPublishedGames() throws Exception {
        when(providers.igdb(eq("companies"), eq(6), contains("developed.name"))).thenReturn(json("""
            {"name":"Company","country":840,"logo":{"image_id":"logo"},
             "developed":[{"id":1,"name":"Game","first_release_date":1577836800}],"published":[{"id":2,"name":"Other"}]}
            """));
        var profile = service.getProfile("igdb-6").orElseThrow();
        assertThat(profile.country()).isEqualTo("840");
        assertThat(profile.logo()).endsWith("/t_1080p/logo.jpg");
        assertThat(profile.portfolio().developedGames().getFirst().releaseYear()).isEqualTo(2020);
        assertThat(profile.portfolio().publishedGames().getFirst().mediaId()).isEqualTo("igdb-game-2");
    }

    @Test
    void secondaryFailureKeepsCompanyAndInvalidSlugsDoNotFetch() throws Exception {
        for (String slug : new String[]{null, "1", "rawg-2", "mal-3", "tmdb-0", "igdb-999999999999"}) {
            assertThat(service.getProfile(slug)).isEmpty();
        }
        verifyNoInteractions(providers);
        when(providers.tmdb("/company/1", Map.of())).thenReturn(json("{\"name\":\"Company\"}"));
        assertThat(service.getProfile("tmdb-1").orElseThrow().portfolio().producedFilmTv()).isEmpty();
        assertThat(service.getProfile("tmdb-2")).isEmpty();
    }
}
