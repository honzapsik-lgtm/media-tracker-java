package com.mediatracker.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final ObjectMapper objectMapper;
    private final AuthService authService;

    @Value("${app.jwt.secret:default-fallback-secret-for-development-change-in-production-123456}")
    private String jwtSecret;

    @Value("${app.gateway.secret:default-internal-secret-change-in-prod-123456}")
    private String gatewaySecret;

    public JwtAuthenticationFilter(ObjectMapper objectMapper, AuthService authService) {
        this.objectMapper = objectMapper;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String xUserId = request.getHeader("X-User-Id");
        String gatewayKey = request.getHeader("X-Internal-Gateway-Key");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            processBearerToken(token, request);
        } else if (gatewayKey != null && gatewayKey.equals(gatewaySecret)) {
            UUID userId = null;
            if (xUserId != null && !xUserId.isBlank()) {
                userId = authService.resolveUserId(xUserId).orElse(null);
            }
            String role = request.getHeader("X-User-Role");
            String email = request.getHeader("X-User-Email");
            SecurityUser user = new SecurityUser(userId, email, role != null ? role : (userId != null ? "user" : "system"));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (xUserId != null && !xUserId.isBlank()) {
            log.warn("Unauthorized internal header spoofing attempt from {}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized: Invalid internal gateway key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void processBearerToken(String token, HttpServletRequest request) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return;

            // Payload is part[1]
            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(decodedBytes);

            String sub = payload.hasNonNull("sub") ? payload.get("sub").asText()
                    : payload.hasNonNull("id") ? payload.get("id").asText()
                    : payload.hasNonNull("userId") ? payload.get("userId").asText()
                    : null;

            if (sub == null) return;

            UUID userId = authService.resolveUserId(sub).orElse(null);
            if (userId == null) {
                log.warn("Could not resolve userId from JWT sub: {}", sub);
                return;
            }

            String email = payload.path("email").asText(null);
            String role = payload.path("role").asText("user");

            SecurityUser user = new SecurityUser(userId, email, role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
        }
    }
}
