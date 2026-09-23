package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "seasons")
public class SeasonEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "\"anilistId\"", unique = true)
    private Integer anilistId;

    @Column(name = "\"tmdbId\"", unique = true)
    private Integer tmdbId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"episodeData\"")
    private JsonNode episodeData;

    @Column(name = "\"mediaId\"", nullable = false)
    private String mediaId;

    @Column(name = "\"releaseDate\"")
    private String releaseDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"staffData\"")
    private JsonNode staffData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"castData\"")
    private JsonNode castData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"studioData\"")
    private JsonNode studioData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"themeData\"")
    private JsonNode themeData;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getAnilistId() { return anilistId; }
    public void setAnilistId(Integer anilistId) { this.anilistId = anilistId; }
    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
    public JsonNode getEpisodeData() { return episodeData; }
    public void setEpisodeData(JsonNode episodeData) { this.episodeData = episodeData; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public JsonNode getStaffData() { return staffData; }
    public void setStaffData(JsonNode staffData) { this.staffData = staffData; }
    public JsonNode getCastData() { return castData; }
    public void setCastData(JsonNode castData) { this.castData = castData; }
    public JsonNode getStudioData() { return studioData; }
    public void setStudioData(JsonNode studioData) { this.studioData = studioData; }
    public JsonNode getThemeData() { return themeData; }
    public void setThemeData(JsonNode themeData) { this.themeData = themeData; }
}
