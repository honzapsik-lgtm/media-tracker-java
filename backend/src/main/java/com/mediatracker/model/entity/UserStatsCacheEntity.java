package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.mediatracker.model.enums.MediaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"UserStatsCache\"")
@IdClass(UserStatsCacheId.class)
public class UserStatsCacheEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stats_json", nullable = false)
    private JsonNode statsJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public JsonNode getStatsJson() { return statsJson; }
    public void setStatsJson(JsonNode statsJson) { this.statsJson = statsJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
