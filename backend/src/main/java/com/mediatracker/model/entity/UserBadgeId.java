package com.mediatracker.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserBadgeId implements Serializable {
    private UUID userId;
    private String badgeId;

    public UserBadgeId() {}
    public UserBadgeId(UUID userId, String badgeId) {
        this.userId = userId;
        this.badgeId = badgeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserBadgeId that = (UserBadgeId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(badgeId, that.badgeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, badgeId);
    }
}
