package com.mediatracker.model.entity;

import java.io.Serializable;
import java.util.Objects;

public class VerificationTokenId implements Serializable {
    private String identifier;
    private String token;

    public VerificationTokenId() {}
    public VerificationTokenId(String identifier, String token) {
        this.identifier = identifier;
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerificationTokenId that = (VerificationTokenId) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, token);
    }
}
