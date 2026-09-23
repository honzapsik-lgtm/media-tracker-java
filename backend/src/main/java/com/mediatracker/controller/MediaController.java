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

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
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
}
