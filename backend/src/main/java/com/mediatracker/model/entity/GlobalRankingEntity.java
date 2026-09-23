package com.mediatracker.model.entity;

import com.mediatracker.model.enums.MediaType;
import jakarta.persistence.*;

@Entity
@Table(name = "global_rankings")
public class GlobalRankingEntity {

    @Id
    @Column(name = "media_id", nullable = false)
    private String mediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "elo_score", nullable = false)
    private Double eloScore = 1200.0;

    @Column(name = "rank")
    private Integer rank;

    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public Double getEloScore() { return eloScore; }
    public void setEloScore(Double eloScore) { this.eloScore = eloScore; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
}
