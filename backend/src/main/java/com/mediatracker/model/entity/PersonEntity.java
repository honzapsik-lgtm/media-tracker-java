package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "people")
public class PersonEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "\"tmdbId\"", unique = true)
    private Integer tmdbId;

    @Column(name = "\"anilistId\"", unique = true)
    private Integer anilistId;

    @Column(name = "\"igdbId\"", unique = true)
    private Integer igdbId;

    @Column(name = "\"malId\"", unique = true)
    private Integer malId;

    @Column(name = "\"rawgId\"", unique = true)
    private Integer rawgId;

    @Column(name = "\"mangadexId\"", unique = true)
    private String mangadexId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "\"nativeName\"")
    private String nativeName;

    @Column(name = "biography")
    private String biography;

    @Column(name = "\"profileImage\"")
    private String profileImage;

    @Column(name = "\"birthDate\"")
    private String birthDate;

    @Column(name = "\"deathDate\"")
    private String deathDate;

    @Column(name = "\"knownForDepartment\"")
    private String knownForDepartment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"mergedCredits\"")
    private JsonNode mergedCredits;

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
    public Integer getMalId() { return malId; }
    public void setMalId(Integer malId) { this.malId = malId; }
    public Integer getRawgId() { return rawgId; }
    public void setRawgId(Integer rawgId) { this.rawgId = rawgId; }
    public String getMangadexId() { return mangadexId; }
    public void setMangadexId(String mangadexId) { this.mangadexId = mangadexId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNativeName() { return nativeName; }
    public void setNativeName(String nativeName) { this.nativeName = nativeName; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getDeathDate() { return deathDate; }
    public void setDeathDate(String deathDate) { this.deathDate = deathDate; }
    public String getKnownForDepartment() { return knownForDepartment; }
    public void setKnownForDepartment(String knownForDepartment) { this.knownForDepartment = knownForDepartment; }
    public JsonNode getMergedCredits() { return mergedCredits; }
    public void setMergedCredits(JsonNode mergedCredits) { this.mergedCredits = mergedCredits; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
