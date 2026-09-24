package com.mediatracker.model.entity;

import com.mediatracker.model.enums.VisibilityLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_privacy_settings")
public class UserPrivacySettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "profile_visibility", nullable = false)
    private VisibilityLevel profileVisibility = VisibilityLevel.PUBLIC;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "ratings_visibility", nullable = false)
    private VisibilityLevel ratingsVisibility = VisibilityLevel.PUBLIC;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "watchlist_visibility", nullable = false)
    private VisibilityLevel watchlistVisibility = VisibilityLevel.PUBLIC;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "activity_visibility", nullable = false)
    private VisibilityLevel activityVisibility = VisibilityLevel.PUBLIC;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public VisibilityLevel getProfileVisibility() { return profileVisibility; }
    public void setProfileVisibility(VisibilityLevel profileVisibility) { this.profileVisibility = profileVisibility; }
    public VisibilityLevel getRatingsVisibility() { return ratingsVisibility; }
    public void setRatingsVisibility(VisibilityLevel ratingsVisibility) { this.ratingsVisibility = ratingsVisibility; }
    public VisibilityLevel getWatchlistVisibility() { return watchlistVisibility; }
    public void setWatchlistVisibility(VisibilityLevel watchlistVisibility) { this.watchlistVisibility = watchlistVisibility; }
    public VisibilityLevel getActivityVisibility() { return activityVisibility; }
    public void setActivityVisibility(VisibilityLevel activityVisibility) { this.activityVisibility = activityVisibility; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
