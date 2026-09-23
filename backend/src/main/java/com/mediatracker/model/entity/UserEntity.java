package com.mediatracker.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "\"emailVerified\"")
    private LocalDateTime emailVerified;

    @Column(name = "image")
    private String image;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"realName\"")
    private String realName;

    @Column(name = "\"stateRegion\"")
    private String stateRegion;

    @Column(name = "country")
    private String country;

    @Column(name = "\"showcaseBadges\"")
    private String[] showcaseBadges = new String[0];

    @Column(name = "role", nullable = false)
    private String role = "user";

    @Column(name = "username", unique = true)
    private String username;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getEmailVerified() { return emailVerified; }
    public void setEmailVerified(LocalDateTime emailVerified) { this.emailVerified = emailVerified; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getStateRegion() { return stateRegion; }
    public void setStateRegion(String stateRegion) { this.stateRegion = stateRegion; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String[] getShowcaseBadges() { return showcaseBadges; }
    public void setShowcaseBadges(String[] showcaseBadges) { this.showcaseBadges = showcaseBadges; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
