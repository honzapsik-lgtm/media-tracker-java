package com.mediatracker.service;

import com.mediatracker.repository.ApiCacheRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminMediaActionsServiceTest {
    private final ApiCacheRepository cache = mock(ApiCacheRepository.class);
    private final StatsService stats = mock(StatsService.class);
    private final AdminMediaActionsService service = new AdminMediaActionsService(cache, stats);

    @Test
    void clearsExactAndBoundaryChildrenButNotNumericNeighbours() {
        when(cache.findIdsWithPrefix("tmdb-tv-12")).thenReturn(List.of(
                "tmdb-tv-12", "tmdb-tv-12-s1", "tmdb-tv-12:credits", "tmdb-tv-123", "tmdb-tv-123-s1"));
        when(cache.findIdsWithPrefix("tmdb-details-v2-tv-12")).thenReturn(List.of(
                "tmdb-details-v2-tv-12", "tmdb-details-v2-tv-123"));
        when(cache.findIdsWithPrefix("tmdb-episodes-12")).thenReturn(List.of(
                "tmdb-episodes-12-1", "tmdb-episodes-123-1"));
        when(cache.findIdsWithPrefix("tmdb-episode-credits-12")).thenReturn(List.of(
                "tmdb-episode-credits-12-1-2", "tmdb-episode-credits-123-1-2"));
        when(cache.findIdsWithPrefix("tmdb-rec-tv-12")).thenReturn(List.of("tmdb-rec-tv-12"));
        Set<String> expected = Set.of("tmdb-tv-12", "tmdb-tv-12-s1", "tmdb-tv-12:credits",
                "tmdb-details-v2-tv-12", "tmdb-episodes-12-1", "tmdb-episode-credits-12-1-2", "tmdb-rec-tv-12");
        when(cache.deleteSelectedIds(expected)).thenReturn(6);
        // Return the database's affected-row count, even if a candidate expired concurrently.
        assertEquals(6, service.clearCache("tmdb-tv-12"));
        verify(cache).deleteSelectedIds(expected);
        verifyNoInteractions(stats);
    }

    @Test
    void seasonDoesNotClearSiblingSeasonsOrParent() {
        when(cache.findIdsWithPrefix("tmdb-episodes-12-1")).thenReturn(List.of(
                "tmdb-episodes-12-1", "tmdb-episodes-12-10"));
        when(cache.findIdsWithPrefix("tmdb-episode-credits-12-1")).thenReturn(List.of(
                "tmdb-episode-credits-12-1-2", "tmdb-episode-credits-12-10-2"));
        service.clearCache("tmdb-tv-12-s1");
        verify(cache).deleteSelectedIds(Set.of("tmdb-episodes-12-1", "tmdb-episode-credits-12-1-2"));
        verify(cache, never()).findIdsWithPrefix("tmdb-details-v2-tv-12");
        verify(cache, never()).findIdsWithPrefix("tmdb-episodes-12");
    }

    @Test
    void episodeDoesNotClearSiblingEpisodesOrSeasonList() {
        when(cache.findIdsWithPrefix("tmdb-episode-credits-12-1-2")).thenReturn(List.of(
                "tmdb-episode-credits-12-1-2", "tmdb-episode-credits-12-1-20"));
        service.clearCache("tmdb-tv-12-s1-e2");
        verify(cache).deleteSelectedIds(Set.of("tmdb-episode-credits-12-1-2"));
        verify(cache, never()).findIdsWithPrefix("tmdb-episodes-12-1");
    }

    @Test
    void mangaDexClearsDetailsAndChapterPages() {
        String uuid = "12345678-1234-1234-1234-123456789abc";
        when(cache.findIdsWithPrefix("mangadex-details-v2-" + uuid)).thenReturn(List.of("mangadex-details-v2-" + uuid));
        when(cache.findIdsWithPrefix("mangadex-chapters-" + uuid)).thenReturn(List.of(
                "mangadex-chapters-" + uuid + "-aggregate", "mangadex-chapters-" + uuid + "-feed?offset=500"));
        service.clearCache("mangadex-manga-" + uuid);
        verify(cache).deleteSelectedIds(Set.of("mangadex-details-v2-" + uuid,
                "mangadex-chapters-" + uuid + "-aggregate", "mangadex-chapters-" + uuid + "-feed?offset=500"));
    }

    @Test
    void anilistAndRawgResolveTheirOwnCacheFamilies() {
        service.clearCache("anilist-manga-12");
        verify(cache).findIdsWithPrefix("anilist-12");
        verify(cache).findIdsWithPrefix("anilist-dto-12");
        service.clearCache("rawg-game-12");
        verify(cache).findIdsWithPrefix("rawg-game-12");
        verify(cache).findIdsWithPrefix("rawg-to-igdb-12");
        verify(cache, never()).deleteSelectedIds(any());
    }

    @Test
    void refreshDelegatesToStatsWithoutTouchingCache() {
        service.refreshStats("tmdb-tv-12-s1-e2");
        verify(stats).refreshMediaStats("tmdb-tv-12-s1-e2", null);
        verifyNoInteractions(cache);
    }

    @Test
    void rejectsBroadPrefixesWildcardsAndInvalidIdsBeforeAnyMutation() {
        for (String id : new String[] { "", "tmdb", "tmdb-tv", "tmdb-tv-%", "tmdb-tv-1_", "../", "mangadex-manga-invalid" }) {
            assertThrows(IllegalArgumentException.class, () -> service.clearCache(id));
            assertThrows(IllegalArgumentException.class, () -> service.refreshStats(id));
        }
        verifyNoInteractions(cache, stats);
    }

    @Test
    void failuresPropagateInsteadOfReportingSuccess() {
        doThrow(new IllegalStateException("stats unavailable")).when(stats).refreshMediaStats("tmdb-movie-12", null);
        assertThrows(IllegalStateException.class, () -> service.refreshStats("tmdb-movie-12"));
        when(cache.findIdsWithPrefix("igdb-game-12")).thenThrow(new IllegalStateException("cache unavailable"));
        assertThrows(IllegalStateException.class, () -> service.clearCache("igdb-game-12"));
    }
}
