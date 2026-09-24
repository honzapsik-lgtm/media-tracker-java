package com.mediatracker.controller;

import com.mediatracker.model.dto.TwinTasteRecommendationDto;
import com.mediatracker.security.SecurityUtils;
import com.mediatracker.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/discover/recommendations")
@Tag(name = "Recommendations", description = "Twin Taste Match and personalized media recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(summary = "Get personalized Twin Taste Match and content-based recommendations for current user")
    public ResponseEntity<TwinTasteRecommendationDto> getRecommendations() {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        TwinTasteRecommendationDto result = recommendationService.getRecommendations(currentUserId);
        return ResponseEntity.ok(result);
    }
}
