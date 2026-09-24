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

class PersonProfileServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProfileProviderClient providers = mock(ProfileProviderClient.class);
    private final PersonProfileService service = new PersonProfileService(providers);
    private Optional<JsonNode> json(String value) throws Exception { return Optional.of(mapper.readTree(value)); }

    @Test
    void tmdbSortsAndDeduplicatesWithoutLosingDifferentCrewJobs() throws Exception {
        when(providers.tmdb(eq("/person/7"), anyMap())).thenReturn(json("""
            {"name":"Actor","also_known_as":["Native"],"birthday":"1970-02-03","profile_path":"/face.jpg",
             "combined_credits":{"cast":[
               {"id":1,"media_type":"movie","title":"Low","popularity":1},
               {"id":2,"media_type":"tv","name":"High","popularity":9,"character":"Hero (voice)","first_air_date":"2020-01-01"},
               {"id":2,"media_type":"tv","name":"Duplicate","popularity":2},
               {"id":3,"media_type":"person"}],
             "crew":[{"id":1,"media_type":"movie","job":"Director"},
                      {"id":1,"media_type":"movie","job":"Writer"},
                      {"id":1,"media_type":"movie","job":"Director"}]}}
            """));
        var profile = service.getProfile(" TMDB-person-007 ").orElseThrow();
        assertThat(profile.id()).isEqualTo("tmdb-7");
        assertThat(profile.tmdbId()).isEqualTo(7);
        assertThat(profile.nativeName()).isEqualTo("Native");
        assertThat(profile.profileImage()).endsWith("/face.jpg");
        assertThat(profile.credits().cast()).extracting(c -> c.mediaId()).containsExactly("tmdb-tv-2", "tmdb-movie-1");
        assertThat(profile.credits().cast().getFirst().isVoiceRole()).isTrue();
        assertThat(profile.credits().cast().getFirst().releaseYear()).isEqualTo(2020);
        assertThat(profile.credits().crew()).extracting(c -> c.role()).containsExactly("Director", "Writer");
        JsonNode serialized = mapper.valueToTree(profile);
        assertThat(serialized.has("bio")).isTrue();
        assertThat(serialized.path("anilistId").isNull()).isTrue();
        assertThat(serialized.at("/credits/cast/0/isVoiceRole").asBoolean()).isTrue();
        assertThat(serialized.at("/credits/cast/0/characterImage").isNull()).isTrue();
    }

    @Test
    void anilistKeepsBothCreditPagesAndCharacterDetails() throws Exception {
        when(providers.anilist(eq("person-8"), anyString(), eq(Map.of("id", 8)), eq(false))).thenReturn(json("""
            {"Staff":{"name":{"full":"Staff","native":"Native"},"dateOfBirth":{"year":1980},
             "primaryOccupations":["Voice Actor"],
             "characterMedia":{"pageInfo":{"hasNextPage":true},"edges":[
               {"characters":[{"name":{"full":"Hero"},"image":{"large":"character.jpg"}}],
                "node":{"id":11,"type":"ANIME","title":{"romaji":"Anime"}}}]},
             "staffMedia":{"pageInfo":{"hasNextPage":true},"edges":[
               {"staffRole":"Story","node":{"id":12,"type":"MANGA","title":{"english":"Manga"}}}]}}}
            """));
        when(providers.anilist(eq("person-8-characterMedia-2"), anyString(), eq(Map.of("id", 8, "page", 2)), eq(true)))
                .thenReturn(json("""
                    {"Staff":{"characterMedia":{"pageInfo":{"hasNextPage":true},"edges":[
                    {"node":{"id":13,"type":"ANIME"}}]}}}
                    """));
        when(providers.anilist(eq("person-8-staffMedia-2"), anyString(), anyMap(), eq(true)))
                .thenReturn(json("""
                    {"Staff":{"staffMedia":{"edges":[{"staffRole":"Director","node":{"id":14,"type":"ANIME"}},{"node":null}]}}}
                    """));
        var profile = service.getProfile("anilist-8").orElseThrow();
        assertThat(profile.birthDate()).isEqualTo("1980-01-01");
        assertThat(profile.credits().cast()).hasSize(2);
        assertThat(profile.credits().crew()).hasSize(2);
        assertThat(profile.credits().cast().getFirst().characterImage()).isEqualTo("character.jpg");
        assertThat(profile.credits().cast().getFirst().role()).isEqualTo("Hero");
        assertThat(profile.credits().cast().getFirst().mediaId()).isEqualTo("anilist-show-11");
        assertThat(profile.credits().crew().getFirst().mediaType()).isEqualTo("MANGA");
        assertThat(profile.credits().crew().getFirst().mediaId()).isEqualTo("anilist-manga-12");
        verify(providers, times(3)).anilist(anyString(), anyString(), anyMap(), anyBoolean());
    }

    @Test
    void anilistPageFailurePreservesInitialCredits() throws Exception {
        when(providers.anilist(eq("person-8"), anyString(), anyMap(), eq(false))).thenReturn(json("""
            {"Staff":{"characterMedia":{"pageInfo":{"hasNextPage":true},"edges":[{"node":{"id":1}}]}}}
            """));
        assertThat(service.getProfile("anilist-8").orElseThrow().credits().cast()).hasSize(1);
    }

    @Test
    void igdbRetainsGamesImagesAndUtcDates() throws Exception {
        when(providers.igdb(eq("persons"), eq(9), contains("credited_games"))).thenReturn(json("""
            {"name":"Creator","dob":315532800,"mug_shot":{"image_id":"face"},
             "credited_games":[{"id":4,"name":"Game","cover":{"image_id":"cover"},"first_release_date":1577836800}]}
            """));
        var profile = service.getProfile("igdb-9").orElseThrow();
        assertThat(profile.igdbId()).isEqualTo(9);
        assertThat(profile.birthDate()).isEqualTo("1980-01-01");
        var credit = profile.credits().crew().getFirst();
        assertThat(credit.mediaId()).isEqualTo("igdb-game-4");
        assertThat(credit.releaseYear()).isEqualTo(2020);
        assertThat(credit.poster()).endsWith("/t_1080p/cover.jpg");
        assertThat(credit.role()).isEqualTo("Developer");
    }

    @Test
    void rawgKeepsSlugAndRoleFallback() throws Exception {
        when(providers.rawg(eq("/creators/10"), anyMap())).thenReturn(json("""
            {"name":"Creator","slug":"creator-name","positions":[{"name":"writer"},{"name":"director"}]}
            """));
        when(providers.rawg("/games", Map.of("creators", "creator-name", "page_size", 40))).thenReturn(json("""
            {"results":[{"id":6,"name":"Game","released":"2021-02-03","background_image":"game.jpg"}]}
            """));
        var profile = service.getProfile("rawg-10").orElseThrow();
        assertThat(profile.rawgSlug()).isEqualTo("creator-name");
        assertThat(profile.credits().crew().getFirst().role()).isEqualTo("[RESOLVING_ROLE]:Writer, Director");
        assertThat(profile.credits().crew().getFirst().mediaId()).isEqualTo("rawg-game-6");
    }

    @Test
    void mangadexCombinesAuthorAndArtistWorksAndSortsByYear() throws Exception {
        String id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        when(providers.mangadex("/author/" + id, Map.of())).thenReturn(json("""
            {"data":{"attributes":{"name":"Creator","biography":{"en":"Biography"},"imageUrl":"face.jpg"}}}
            """));
        when(providers.mangadex(eq("/manga"), argThat(m -> m.containsKey("authors[]")))).thenReturn(json("""
            {"data":[{"id":"one","attributes":{"title":{"ja-ro":"First"},"year":2000},
             "relationships":[{"type":"cover_art","attributes":{"fileName":"cover.jpg"}}]}]}
            """));
        when(providers.mangadex(eq("/manga"), argThat(m -> m.containsKey("artists[]")))).thenReturn(json("""
            {"data":[{"id":"one"},{"id":"two","attributes":{"title":{"fr":"Second"},"createdAt":"2023-01-01"}}]}
            """));
        var profile = service.getProfile(id.toUpperCase()).orElseThrow();
        assertThat(profile.mangadexId()).isEqualTo(id);
        assertThat(profile.bio()).isEqualTo("Biography");
        assertThat(profile.credits().crew()).extracting(c -> c.title()).containsExactly("Second", "First");
        assertThat(profile.credits().crew().get(1).poster()).isEqualTo("https://uploads.mangadex.org/covers/one/cover.jpg.512.jpg");
    }

    @Test
    void invalidUnsupportedAndMissingProfilesReturnEmpty() {
        for (String slug : new String[]{null, "bad", "tmdb-0", "tmdb-99999999999999999", "mal-3", "tmdb-1;drop"}) {
            assertThat(service.getProfile(slug)).isEmpty();
        }
        verifyNoInteractions(providers);
        assertThat(service.getProfile("123")).isEmpty();
        verify(providers).tmdb(eq("/person/123"), anyMap());
    }
}
