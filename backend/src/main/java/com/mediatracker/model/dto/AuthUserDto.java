package com.mediatracker.model.dto;

import java.util.UUID;

public record AuthUserDto(
    UUID id,
    String username,
    String role,
    String email,
    String name,
    String image
) {}
