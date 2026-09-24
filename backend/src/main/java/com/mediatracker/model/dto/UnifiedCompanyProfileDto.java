package com.mediatracker.model.dto;

import java.util.List;

public record UnifiedCompanyProfileDto(String id, String name, String description, String logo,
                                       String country, Portfolio portfolio) {
    public record Portfolio(List<ProfileMediaDto> developedGames, List<ProfileMediaDto> publishedGames,
                            List<ProfileMediaDto> animationStudioFor, List<ProfileMediaDto> producedAnime,
                            List<ProfileMediaDto> publishedManga, List<ProfileMediaDto> producedFilmTv,
                            List<ProfileMediaDto> broadcastedOn) {}
}
