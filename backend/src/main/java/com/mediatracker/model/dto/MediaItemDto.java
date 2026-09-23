package com.mediatracker.model.dto;

import java.util.ArrayList;
import java.util.List;

public class MediaItemDto {
    private String id;
    private String title;
    private String originalTitle;
    private String originalLanguage;
    private String type; // "movie", "show", "game", "manga"
    private String image;
    private String backdrop;
    private String description;
    private String releaseDate;
    private Integer globalScore;
    private Integer runtime;
    private List<String> genres = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private String trailerUrl;
    private List<MediaCreditDto> cast = new ArrayList<>();
    private List<MediaCreditDto> credits = new ArrayList<>();
    private Object watchData;
    private Object seasons;
    private Integer chapters;
    private Integer volumes;
    private String status;
    private Object themeData;
    private List<Object> companies = new ArrayList<>();
    private List<Object> characters = new ArrayList<>();
    private List<String> engines = new ArrayList<>();
    private List<Object> playLinks = new ArrayList<>();
    private Object relatedManga;
    private List<Object> canonMovies = new ArrayList<>();
    private List<Object> externalLinks = new ArrayList<>();
    private String origin;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getBackdrop() { return backdrop; }
    public void setBackdrop(String backdrop) { this.backdrop = backdrop; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public Integer getGlobalScore() { return globalScore; }
    public void setGlobalScore(Integer globalScore) { this.globalScore = globalScore; }

    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer runtime) { this.runtime = runtime; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    public List<MediaCreditDto> getCast() { return cast; }
    public void setCast(List<MediaCreditDto> cast) { this.cast = cast; }

    public List<MediaCreditDto> getCredits() { return credits; }
    public void setCredits(List<MediaCreditDto> credits) { this.credits = credits; }

    public Object getWatchData() { return watchData; }
    public void setWatchData(Object watchData) { this.watchData = watchData; }

    public Object getSeasons() { return seasons; }
    public void setSeasons(Object seasons) { this.seasons = seasons; }

    public Integer getChapters() { return chapters; }
    public void setChapters(Integer chapters) { this.chapters = chapters; }

    public Integer getVolumes() { return volumes; }
    public void setVolumes(Integer volumes) { this.volumes = volumes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getThemeData() { return themeData; }
    public void setThemeData(Object themeData) { this.themeData = themeData; }

    public List<Object> getCompanies() { return companies; }
    public void setCompanies(List<Object> companies) { this.companies = companies; }

    public List<Object> getCharacters() { return characters; }
    public void setCharacters(List<Object> characters) { this.characters = characters; }

    public List<String> getEngines() { return engines; }
    public void setEngines(List<String> engines) { this.engines = engines; }

    public List<Object> getPlayLinks() { return playLinks; }
    public void setPlayLinks(List<Object> playLinks) { this.playLinks = playLinks; }

    public Object getRelatedManga() { return relatedManga; }
    public void setRelatedManga(Object relatedManga) { this.relatedManga = relatedManga; }

    public List<Object> getCanonMovies() { return canonMovies; }
    public void setCanonMovies(List<Object> canonMovies) { this.canonMovies = canonMovies; }

    public List<Object> getExternalLinks() { return externalLinks; }
    public void setExternalLinks(List<Object> externalLinks) { this.externalLinks = externalLinks; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
}