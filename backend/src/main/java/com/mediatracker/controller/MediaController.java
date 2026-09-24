package com.mediatracker.controller;

import com.mediatracker.model.dto.EpisodeDto;
import com.mediatracker.model.dto.MediaItemDto;
import com.mediatracker.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media", description = "Endpoints for retrieving unified media metadata and episodes")
public class MediaController {

    private final MediaService mediaService;
    private final com.mediatracker.repository.MediaStatsRepository mediaStatsRepository;
    private final com.mediatracker.repository.GlobalRankingRepository globalRankingRepository;

    public MediaController(MediaService mediaService,
                           com.mediatracker.repository.MediaStatsRepository mediaStatsRepository,
                           com.mediatracker.repository.GlobalRankingRepository globalRankingRepository) {
        this.mediaService = mediaService;
        this.mediaStatsRepository = mediaStatsRepository;
        this.globalRankingRepository = globalRankingRepository;
    }

    @PostMapping("/batch-stats")
    @Operation(summary = "Batch retrieve community scores and global list ranks for media IDs")
    public ResponseEntity<?> getBatchStats(@RequestBody(required = false) java.util.Map<String, List<String>> body) {
        List<String> ids = body != null ? body.getOrDefault("ids", List.of()) : List.of();
        return resolveBatchStats(ids);
    }

    @GetMapping("/batch-stats")
    @Operation(summary = "Batch retrieve community scores and global list ranks via query params")
    public ResponseEntity<?> getBatchStatsGet(@RequestParam(name = "ids", defaultValue = "") List<String> ids) {
        return resolveBatchStats(ids);
    }

    private ResponseEntity<?> resolveBatchStats(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(java.util.Map.of("stats", java.util.Map.of(), "ranks", java.util.Map.of()));
        }

        java.util.Map<String, Integer> statsMap = mediaStatsRepository.findAllById(ids)
                .stream()
                .filter(s -> s.getCommunityAverage() != null && s.getTotalRatings() != null && s.getTotalRatings() > 0)
                .collect(java.util.stream.Collectors.toMap(
                        com.mediatracker.model.entity.MediaStatsEntity::getId,
                        s -> s.getCommunityAverage().intValue(),
                        (a, b) -> a
                ));

        java.util.Map<String, Integer> rankMap = globalRankingRepository.findAllById(ids)
                .stream()
                .filter(r -> r.getRank() != null)
                .collect(java.util.stream.Collectors.toMap(
                        com.mediatracker.model.entity.GlobalRankingEntity::getMediaId,
                        com.mediatracker.model.entity.GlobalRankingEntity::getRank,
                        (a, b) -> a
                ));

        return ResponseEntity.ok(java.util.Map.of("stats", statsMap, "ranks", rankMap));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get full media details for a canonical 3-part slug")
    public ResponseEntity<MediaItemDto> getMediaDetails(@PathVariable String slug) {
        return mediaService.getMediaDetails(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}/season/{seasonNumber}")
    @Operation(summary = "Get episodes for a TV show season with anime specials integration")
    public ResponseEntity<List<EpisodeDto>> getSeasonEpisodes(@PathVariable String slug,
                                                              @PathVariable int seasonNumber) {
        List<EpisodeDto> episodes = mediaService.getSeasonEpisodes(slug, seasonNumber);
        return ResponseEntity.ok(episodes);
    }

    @GetMapping("/{slug}/season/{seasonNumber}/themes")
    public Object getSeasonThemes(@PathVariable String slug, @PathVariable int seasonNumber) {
        return mediaService.getSeasonThemes(slug, seasonNumber);
    }

    @GetMapping("/{slug}/season/{seasonNumber}/episode/{episodeNumber}/credits")
    public Object getEpisodeCredits(@PathVariable String slug, @PathVariable int seasonNumber, @PathVariable int episodeNumber) {
        return mediaService.getEpisodeCredits(slug, seasonNumber, episodeNumber);
    }

    @GetMapping("/{slug}/chapters")
    public Object getChapters(@PathVariable String slug, @RequestParam(defaultValue = "0") int offset) {
        return mediaService.getChapterFeed(slug, offset);
    }

    @GetMapping("/{slug}/chapter-metadata")
    public Object getChapterMetadata(@PathVariable String slug) {
        return mediaService.getChapterMetadata(slug);
    }
}
