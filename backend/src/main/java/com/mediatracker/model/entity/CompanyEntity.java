package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "companies")
public class CompanyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "\"tmdbId\"", unique = true)
    private Integer tmdbId;

    @Column(name = "\"anilistId\"", unique = true)
    private Integer anilistId;

    @Column(name = "\"igdbId\"", unique = true)
    private Integer igdbId;

    @Column(name = "\"tmdbNetworkId\"", unique = true)
    private Integer tmdbNetworkId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "\"logoUrl\"")
    private String logoUrl;

    @Column(name = "country")
    private String country;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"mergedWorks\"")
    private JsonNode mergedWorks;

    @Column(name = "\"createdAt\"", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"updatedAt\"", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
    public Integer getAnilistId() { return anilistId; }
    public void setAnilistId(Integer anilistId) { this.anilistId = anilistId; }
    public Integer getIgdbId() { return igdbId; }
    public void setIgdbId(Integer igdbId) { this.igdbId = igdbId; }
    public Integer getTmdbNetworkId() { return tmdbNetworkId; }
    public void setTmdbNetworkId(Integer tmdbNetworkId) { this.tmdbNetworkId = tmdbNetworkId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public JsonNode getMergedWorks() { return mergedWorks; }
    public void setMergedWorks(JsonNode mergedWorks) { this.mergedWorks = mergedWorks; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
