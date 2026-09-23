package com.mediatracker.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_friend_preferences")
public class UserFriendPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "friend_id", nullable = false)
    private UUID friendId;

    @Column(name = "hide_activity", nullable = false)
    private boolean hideActivity = false;

    @Column(name = "hide_ratings", nullable = false)
    private boolean hideRatings = false;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getFriendId() { return friendId; }
    public void setFriendId(UUID friendId) { this.friendId = friendId; }
    public boolean isHideActivity() { return hideActivity; }
    public void setHideActivity(boolean hideActivity) { this.hideActivity = hideActivity; }
    public boolean isHideRatings() { return hideRatings; }
    public void setHideRatings(boolean hideRatings) { this.hideRatings = hideRatings; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
