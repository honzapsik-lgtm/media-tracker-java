package com.mediatracker.model.dto;

public class EpisodeDto {
    private Integer id;
    private String name;
    private Integer episodeNumber;
    private String overview;
    private String image;
    private String airDate;
    private Integer runtime;
    private Integer globalScore;
    private Boolean isFinaleSpecial = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getAirDate() { return airDate; }
    public void setAirDate(String airDate) { this.airDate = airDate; }
    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer runtime) { this.runtime = runtime; }
    public Integer getGlobalScore() { return globalScore; }
    public void setGlobalScore(Integer globalScore) { this.globalScore = globalScore; }
    public Boolean getIsFinaleSpecial() { return isFinaleSpecial; }
    public void setIsFinaleSpecial(Boolean isFinaleSpecial) { this.isFinaleSpecial = isFinaleSpecial; }
    public void setFinaleSpecial(Boolean isFinaleSpecial) { this.isFinaleSpecial = isFinaleSpecial; }
}