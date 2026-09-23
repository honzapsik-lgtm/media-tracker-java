package com.mediatracker.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CreditsParser {

    private static final Pattern DIRECTOR_PATTERN = Pattern.compile("^(director|series director|chief director)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORY_ART_PATTERN = Pattern.compile("^(story & art|mangaka|story and art)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^(author|story|original story)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARTIST_PATTERN = Pattern.compile("^(artist|art|illustrator|illustration)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATOR_PATTERN = Pattern.compile("^(original creator|original concept|creator|original plan)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WRITER_PATTERN = Pattern.compile("^(series composition|head writer)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAR_DESIGN_PATTERN = Pattern.compile("^(character design|original character design)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MUSIC_PATTERN = Pattern.compile("^(music|composer|soundtrack)$", Pattern.CASE_INSENSITIVE);

    public String getGodTierRole(String role) {
        if (role == null || role.isBlank()) return null;
        String lower = role.trim().toLowerCase();

        if (DIRECTOR_PATTERN.matcher(lower).matches()) return "Director";
        if (STORY_ART_PATTERN.matcher(lower).matches() || lower.contains("story & art") || lower.contains("story and art")) return "Story & Art";
        if (AUTHOR_PATTERN.matcher(lower).matches()) return "Author";
        if (ARTIST_PATTERN.matcher(lower).matches()) return "Artist";
        if (CREATOR_PATTERN.matcher(lower).matches()) return "Original Creator";
        if (WRITER_PATTERN.matcher(lower).matches()) return "Series Composition";
        if (CHAR_DESIGN_PATTERN.matcher(lower).matches()) return "Character Design";
        if (MUSIC_PATTERN.matcher(lower).matches()) return "Composer";

        return null;
    }

    public String normalizeTMDbRole(String role) {
        if (role == null || role.isBlank()) return "Unknown";
        String lower = role.trim().toLowerCase();

        if ("director of photography".equals(lower)) return "Cinematographer";
        if ("original music composer".equals(lower)) return "Composer";
        if ("screenplay".equals(lower)) return "Writer";
        if ("novel".equals(lower) || "comic book".equals(lower) || "author".equals(lower)) return "Original Creator";

        String godTier = getGodTierRole(role);
        if (godTier != null) return godTier;

        return role;
    }
}