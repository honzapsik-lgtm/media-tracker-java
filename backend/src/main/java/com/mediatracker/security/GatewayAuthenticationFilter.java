package com.mediatracker.security;

import com.mediatracker.model.dto.AuthUserDto;
import com.mediatracker.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService authService;
    private final byte[] gatewaySecret;

    public GatewayAuthenticationFilter(AuthService authService,
                                       @Value("${app.gateway.secret}") String gatewaySecret) {
        if (gatewaySecret == null || gatewaySecret.isBlank()) {
            throw new IllegalArgumentException("INTERNAL_GATEWAY_SECRET must be configured");
        }
        this.authService = authService;
        this.gatewaySecret = gatewaySecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader("X-Internal-Gateway-Key");
        String userId = request.getHeader("X-User-Id");
        if (key == null) {
            if (userId != null || request.getHeader("X-User-Role") != null
                    || request.getHeader("Authorization") != null) {
                unauthorized(response);
                return;
            }
            chain.doFilter(request, response);
            return;
        }
        if (!MessageDigest.isEqual(gatewaySecret, key.getBytes(StandardCharsets.UTF_8))) {
            unauthorized(response);
            return;
        }

        // The gateway may sync OAuth accounts, but an anonymous gateway is not an admin.
        SecurityUser principal = new SecurityUser(null, null, "gateway");
        if (userId != null && !userId.isBlank()) {
            AuthUserDto user;
            try {
                user = authService.getUserDto(UUID.fromString(userId)).orElse(null);
            } catch (IllegalArgumentException e) {
                unauthorized(response);
                return;
            }
            if (user == null) {
                unauthorized(response);
                return;
            }
            principal = new SecurityUser(user.id(), user.email(), user.role());
        }
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Invalid gateway authentication\"}");
    }
}
