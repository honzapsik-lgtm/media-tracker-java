package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "episodes")
public class EpisodeEntity {

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
}
