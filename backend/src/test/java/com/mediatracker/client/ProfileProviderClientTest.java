package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProfileProviderClientTest {
    private final CacheService cache = mock(CacheService.class);
    private final IgdbClient igdb = mock(IgdbClient.class);
    private MockRestServiceServer server;
    private RestClient rest;
    private ProfileProviderClient client;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        rest = builder.build();
        client = new ProfileProviderClient(rest, cache, igdb, "tmdb-key", "rawg-key", "twitch-id");
    }

    @Test
    void tmdbUsesCredentialsAndOneHourCache() {
        server.expect(requestTo("https://api.themoviedb.org/3/person/1?api_key=tmdb-key"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"name\":\"Person\"}", MediaType.APPLICATION_JSON));
        assertThat(client.tmdb("/person/1", Map.of()).orElseThrow().path("name").asText()).isEqualTo("Person");
        verify(cache).put(anyString(), eq("tmdb"), any(JsonNode.class), eq(3600));
        server.verify();
    }

    @Test
    void collectionQueryValuesAreRepeatedAndCacheKeysAreStable() {
        var params = new LinkedHashMap<String, Object>();
        params.put("limit", 100);
        params.put("includes[]", List.of("cover_art", "author"));
        server.expect(requestTo("https://api.mangadex.org/manga?includes%5B%5D=cover_art&includes%5B%5D=author&limit=100"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mangadex.org/manga?includes%5B%5D=cover_art&includes%5B%5D=author&limit=100"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        assertThat(client.mangadex("/manga", params)).isPresent();
        assertThat(client.mangadex("/manga", Map.of("includes[]", List.of("cover_art", "author"), "limit", 100))).isPresent();
        verify(cache, times(2)).get("profile-v1-mangadex-/manga{includes[]=[cover_art, author], limit=100}", JsonNode.class);
        server.verify();
    }

    @Test
    void cacheHitDoesNotUseNetwork() throws Exception {
        when(cache.get(anyString(), eq(JsonNode.class))).thenReturn(Optional.of(new ObjectMapper().readTree("{\"name\":\"Cached\"}")));
        assertThat(client.rawg("/creators/1", Map.of()).orElseThrow().path("name").asText()).isEqualTo("Cached");
        server.verify();
    }

    @Test
    void missingCredentialsDoNotFetch() {
        var unconfigured = new ProfileProviderClient(rest, cache, igdb, "", "", "");
        assertThat(unconfigured.tmdb("/person/1", Map.of())).isEmpty();
        assertThat(unconfigured.rawg("/creators/1", Map.of())).isEmpty();
        assertThat(unconfigured.igdb("persons", 1, "name")).isEmpty();
        server.verify();
    }

    @Test
    void igdbReusesTokenManagerAndRequestsExpandedFields() {
        when(igdb.getAccessToken()).thenReturn("shared-token");
        server.expect(requestTo("https://api.igdb.com/v4/persons"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Client-ID", "twitch-id"))
                .andExpect(header("Authorization", "Bearer shared-token"))
                .andExpect(content().string("fields name, credited_games.name; where id = 2;"))
                .andRespond(withSuccess("[{\"name\":\"Person\"}]", MediaType.APPLICATION_JSON));
        assertThat(client.igdb("persons", 2, "name, credited_games.name")).isPresent();
        verify(igdb).getAccessToken();
        server.verify();
    }

    @Test
    void anilistRetriesRateLimitedContinuationPages() {
        server.expect(requestTo("https://graphql.anilist.co"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "0"));
        server.expect(requestTo("https://graphql.anilist.co"))
                .andExpect(content().json("{\"query\":\"query\",\"variables\":{\"id\":3,\"page\":2}}"))
                .andRespond(withSuccess("{\"data\":{\"Staff\":{\"id\":3}}}", MediaType.APPLICATION_JSON));
        assertThat(client.anilist("page-2", "query", Map.of("id", 3, "page", 2), true)).isPresent();
        server.verify();
    }

    @Test
    void anilistStopsAfterThreeRateLimitAttempts() {
        for (int i = 0; i < 3; i++) server.expect(requestTo("https://graphql.anilist.co"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "0"));
        assertThat(client.anilist("page-2", "query", Map.of(), true)).isEmpty();
        verify(cache, never()).put(anyString(), anyString(), any(), anyInt());
        server.verify();
    }

    @Test
    void graphQlAndHttpFailuresAreNotCached() {
        server.expect(requestTo("https://graphql.anilist.co"))
                .andRespond(withSuccess("{\"errors\":[{\"message\":\"failed\"}],\"data\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mangadex.org/author/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(client.anilist("person-1", "query", Map.of(), false)).isEmpty();
        assertThat(client.mangadex("/author/missing", Map.of())).isEmpty();
        verify(cache, never()).put(anyString(), anyString(), any(), anyInt());
        server.verify();
    }
}
