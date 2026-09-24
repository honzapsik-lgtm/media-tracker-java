package com.mediatracker.controller;

import com.mediatracker.model.dto.AuthUserDto;
import com.mediatracker.model.dto.OAuthSyncRequest;
import com.mediatracker.security.SecurityUtils;
import com.mediatracker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication and OAuth user synchronization endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/oauth-sync")
    @Operation(summary = "Synchronize OAuth provider account with internal user database")
    public ResponseEntity<?> oauthSync(@RequestBody OAuthSyncRequest request) {
        try {
            AuthUserDto user = authService.syncOAuthUser(request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's profile and credentials")
    public ResponseEntity<?> getCurrentUser() {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        return authService.getUserDto(currentUserId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
