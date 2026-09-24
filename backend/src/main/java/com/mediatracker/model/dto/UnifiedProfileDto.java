package com.mediatracker.model.dto;

import java.util.List;

public record UnifiedProfileDto(
        String id, Integer tmdbId, Integer anilistId, Integer igdbId, Integer malId, Integer rawgId,
        String rawgSlug, String mangadexId, String name, String nativeName, String bio,
        String profileImage, String birthDate, String deathDate, String knownForDepartment, Credits credits) {
    public record Credit(String mediaId, String mediaType, String title, String poster, Integer releaseYear,
                         String role, boolean isVoiceRole, String characterImage) {
        public Credit(ProfileMediaDto media, String role, boolean voice, String characterImage) {
            this(media.mediaId(), media.mediaType(), media.title(), media.poster(), media.releaseYear(),
                    role, voice, characterImage);
        }
    }
    public record Credits(List<Credit> cast, List<Credit> crew) {}
}
