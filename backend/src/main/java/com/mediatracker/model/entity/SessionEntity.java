package com.mediatracker.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"Session\"")
public class SessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "\"sessionToken\"", nullable = false, unique = true)
    private String sessionToken;

    @Column(name = "\"userId\"", nullable = false)
    private UUID userId;

    @Column(name = "expires", nullable = false)
    private LocalDateTime expires;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public LocalDateTime getExpires() { return expires; }
    public void setExpires(LocalDateTime expires) { this.expires = expires; }
}
