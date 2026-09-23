package com.mediatracker.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"VerificationToken\"")
@IdClass(VerificationTokenId.class)
public class VerificationTokenEntity {

    @Id
    @Column(name = "identifier", nullable = false)
    private String identifier;

    @Id
    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "expires", nullable = false)
    private LocalDateTime expires;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpires() { return expires; }
    public void setExpires(LocalDateTime expires) { this.expires = expires; }
}
