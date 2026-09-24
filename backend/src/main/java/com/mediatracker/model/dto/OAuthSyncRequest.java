package com.mediatracker.model.dto;

public record OAuthSyncRequest(
    String provider,
    String providerAccountId,
    String email,
    String name,
    String image
) {}
