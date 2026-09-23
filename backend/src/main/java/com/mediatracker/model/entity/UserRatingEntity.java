package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_ratings")
public class UserRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "media_id", nullable = false)
    private String mediaId;

    @Column(name = "media_title")
    private String mediaTitle;

    @Column(name = "media_image")
    private String mediaImage;

    @Column(name = "media_release_date")
    private String mediaReleaseDate;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "review_text")
    private String reviewText;

    @Column(name = "username")
    private String username;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_scores")
    private JsonNode criteriaScores;

    @Column(name = "is_deep_review")
    private Boolean isDeepReview = false;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public String getMediaTitle() { return mediaTitle; }
    public void setMediaTitle(String mediaTitle) { this.mediaTitle = mediaTitle; }
    public String getMediaImage() { return mediaImage; }
    public void setMediaImage(String mediaImage) { this.mediaImage = mediaImage; }
    public String getMediaReleaseDate() { return mediaReleaseDate; }
    public void setMediaReleaseDate(String mediaReleaseDate) { this.mediaReleaseDate = mediaReleaseDate; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public JsonNode getCriteriaScores() { return criteriaScores; }
    public void setCriteriaScores(JsonNode criteriaScores) { this.criteriaScores = criteriaScores; }
    public Boolean getIsDeepReview() { return isDeepReview; }
    public void setIsDeepReview(Boolean isDeepReview) { this.isDeepReview = isDeepReview; }
    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRatingEntity that = (UserRatingEntity) o;
        return java.util.Objects.equals(userId, that.userId) &&
               java.util.Objects.equals(mediaId, that.mediaId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userId, mediaId);
    }
}
