package com.mediatracker.model.entity;

import com.mediatracker.model.enums.MediaType;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserStatsCacheId implements Serializable {
    private UUID userId;
    private MediaType mediaType;

    public UserStatsCacheId() {}
    public UserStatsCacheId(UUID userId, MediaType mediaType) {
        this.userId = userId;
        this.mediaType = mediaType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStatsCacheId that = (UserStatsCacheId) o;
        return Objects.equals(userId, that.userId) && mediaType == that.mediaType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, mediaType);
    }
}
