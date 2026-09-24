package com.mediatracker.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.service.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MangaDexChapterTest {
    private static final String ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void servesFeedAndAggregateThroughCachedBackendClient() throws Exception {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var cache = mock(CacheService.class);
        var mapper = new ObjectMapper();
        var client = new MangaDexClient(builder.build(), cache, mapper);
        server.expect(request -> {
                    assertThat(request.getURI().getPath()).isEqualTo("/manga/" + ID + "/feed");
                    assertThat(request.getURI().getQuery()).isEqualTo("translatedLanguage[]=en&limit=500&offset=500&order[volume]=asc&order[chapter]=asc");
                })
                .andRespond(withSuccess("{\"data\":[{\"id\":\"chapter-id\",\"attributes\":{\"chapter\":\"12\"}}],\"total\":501}", MediaType.APPLICATION_JSON));
        assertThat(client.getChapterFeed(ID, 500).path("data").get(0).path("id").asText()).isEqualTo("chapter-id");
        JsonNode metadata = mapper.readTree("{\"volumes\":{\"2\":{\"chapters\":{\"12\":{}}}}}");
        when(cache.get("mangadex-chapters-" + ID + "-aggregate", JsonNode.class)).thenReturn(Optional.of(metadata));
        assertThat(client.getChapterMetadata(ID)).isEqualTo(metadata);
        server.verify();
    }

    @Test
    void rejectsMalformedIdsAndOffsetsBeforeNetworkAccess() {
        var client = new MangaDexClient(RestClient.create(), mock(CacheService.class), new ObjectMapper());
        assertThatThrownBy(() -> client.getChapterFeed(ID, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.getChapterFeed(ID, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.getChapterFeed(ID, 10000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.getChapterMetadata("../invalid")).isInstanceOf(IllegalArgumentException.class);
    }
}
