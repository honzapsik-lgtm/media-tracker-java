package com.mediatracker.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UUID> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return Optional.ofNullable(securityUser.getId());
        } else if (principal instanceof String strId) {
            try {
                return Optional.of(UUID.fromString(strId));
            } catch (IllegalArgumentException ignored) {}
        }

        return Optional.empty();
    }

    public static UUID requireCurrentUserId() {
        return getCurrentUserId().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required"));
    }
}
