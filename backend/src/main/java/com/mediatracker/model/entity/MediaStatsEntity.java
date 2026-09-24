package com.mediatracker.model.entity;

import com.mediatracker.model.enums.MediaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;

@Entity
@Table(name = "media_stats")
public class MediaStatsEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "community_average")
    private BigDecimal communityAverage = BigDecimal.ZERO;

    @Column(name = "total_ratings")
    private Integer totalRatings = 0;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getCommunityAverage() { return communityAverage; }
    public void setCommunityAverage(BigDecimal communityAverage) { this.communityAverage = communityAverage; }
    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
}
