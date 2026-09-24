package com.mediatracker.model.dto;

import java.util.List;
import java.util.UUID;

public class TwinTasteRecommendationDto {
    public record TwinProfile(
            UUID id,
            String username,
            String name,
            String image,
            int matchPercentage,
            int sharedRatingsCount
    ) {}

    public record TwinMediaRecommendation(
            String id,
            String title,
            String image,
            String type,
            String releaseDate,
            Integer communityScore,
            Integer listRank,
            int twinScore,
            TwinProfile twin
    ) {}

    public record ContentRecommendation(
            String sourceMediaId,
            String sourceTitle,
            int sourceScore,
            List<DiscoverItemDto> recommendations
    ) {}

    private boolean authenticated;
    private boolean hasRatings;
    private int userRatingsCount;
    private List<TwinProfile> topTwins;
    private List<TwinMediaRecommendation> twinRecommendations;
    private ContentRecommendation contentRecommendation;

    public TwinTasteRecommendationDto() {}

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public boolean isHasRatings() { return hasRatings; }
    public void setHasRatings(boolean hasRatings) { this.hasRatings = hasRatings; }

    public int getUserRatingsCount() { return userRatingsCount; }
    public void setUserRatingsCount(int userRatingsCount) { this.userRatingsCount = userRatingsCount; }

    public List<TwinProfile> getTopTwins() { return topTwins; }
    public void setTopTwins(List<TwinProfile> topTwins) { this.topTwins = topTwins; }

    public List<TwinMediaRecommendation> getTwinRecommendations() { return twinRecommendations; }
    public void setTwinRecommendations(List<TwinMediaRecommendation> twinRecommendations) { this.twinRecommendations = twinRecommendations; }

    public ContentRecommendation getContentRecommendation() { return contentRecommendation; }
    public void setContentRecommendation(ContentRecommendation contentRecommendation) { this.contentRecommendation = contentRecommendation; }
}
