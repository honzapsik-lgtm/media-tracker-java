package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.mediatracker.model.enums.MediaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
public class MediaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "\"anilistId\"", unique = true)
    private Integer anilistId;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MediaType type;

    @Column(name = "\"isMainStoryline\"", nullable = false)
    private boolean isMainStoryline = false;

    @Column(name = "\"releaseDate\"")
    private String releaseDate;

    @Column(name = "\"tmdbId\"", unique = true)
    private Integer tmdbId;

    @Column(name = "\"igdbId\"", unique = true)
    private Integer igdbId;

    @Column(name = "\"mangadexId\"", unique = true)
    private String mangadexId;

    @Column(name = "\"malId\"", unique = true)
    private Integer malId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"themeData\"")
    private JsonNode themeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"watchData\"")
    private JsonNode watchData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"episodeData\"")
    private JsonNode episodeData;

    @Column(name = "\"relatedMediaId\"")
    private String relatedMediaId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"staffData\"")
    private JsonNode staffData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"castData\"")
    private JsonNode castData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"studioData\"")
    private JsonNode studioData;

    @Column(name = "\"franchiseSyncedAt\"")
    private LocalDateTime franchiseSyncedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getAnilistId() { return anilistId; }
    public void setAnilistId(Integer anilistId) { this.anilistId = anilistId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public MediaType getType() { return type; }
    public void setType(MediaType type) { this.type = type; }
    public boolean isMainStoryline() { return isMainStoryline; }
    public void setMainStoryline(boolean mainStoryline) { isMainStoryline = mainStoryline; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
    public Integer getIgdbId() { return igdbId; }
    public void setIgdbId(Integer igdbId) { this.igdbId = igdbId; }
    public String getMangadexId() { return mangadexId; }
    public void setMangadexId(String mangadexId) { this.mangadexId = mangadexId; }
    public Integer getMalId() { return malId; }
    public void setMalId(Integer malId) { this.malId = malId; }
    public JsonNode getThemeData() { return themeData; }
    public void setThemeData(JsonNode themeData) { this.themeData = themeData; }
    public JsonNode getWatchData() { return watchData; }
    public void setWatchData(JsonNode watchData) { this.watchData = watchData; }
    public JsonNode getEpisodeData() { return episodeData; }
    public void setEpisodeData(JsonNode episodeData) { this.episodeData = episodeData; }
    public String getRelatedMediaId() { return relatedMediaId; }
    public void setRelatedMediaId(String relatedMediaId) { this.relatedMediaId = relatedMediaId; }
    public JsonNode getStaffData() { return staffData; }
    public void setStaffData(JsonNode staffData) { this.staffData = staffData; }
    public JsonNode getCastData() { return castData; }
    public void setCastData(JsonNode castData) { this.castData = castData; }
    public JsonNode getStudioData() { return studioData; }
    public void setStudioData(JsonNode studioData) { this.studioData = studioData; }
    public LocalDateTime getFranchiseSyncedAt() { return franchiseSyncedAt; }
    public void setFranchiseSyncedAt(LocalDateTime franchiseSyncedAt) { this.franchiseSyncedAt = franchiseSyncedAt; }
}
