package com.mediatracker.security;

import com.mediatracker.model.dto.AuthUserDto;
import com.mediatracker.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayAuthenticationFilterTest {
    private final AuthService authService = mock(AuthService.class);
    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter(authService, "test-gateway-secret");

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void rejectsLegacyBearerClaims() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer e30.eyJyb2xlIjoiYWRtaW4ifQ.unsigned");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Unsigned claims must not authenticate"));
        assertEquals(401, response.getStatus());
        verifyNoInteractions(authService);
    }

    @Test
    void ignoresForwardedRoleAndUsesDatabaseRole() throws Exception {
        var id = UUID.randomUUID();
        when(authService.getUserDto(id)).thenReturn(Optional.of(new AuthUserDto(id, "member", "user", null, null, null)));
        var request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Gateway-Key", "test-gateway-secret");
        request.addHeader("X-User-Id", id.toString());
        request.addHeader("X-User-Role", "admin");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
        });
    }

    @Test
    void anonymousGatewayCannotBecomeSystemAdmin() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Gateway-Key", "test-gateway-secret");
        request.addHeader("X-User-Role", "system");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("ROLE_GATEWAY", auth.getAuthorities().iterator().next().getAuthority());
        });
    }

    @Test
    void rejectsInvalidGatewayAndMissingUsers() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Gateway-Key", "wrong");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Invalid gateway accepted"));
        assertEquals(401, response.getStatus());
        var id = UUID.randomUUID();
        when(authService.getUserDto(id)).thenReturn(Optional.empty());
        request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Gateway-Key", "test-gateway-secret");
        request.addHeader("X-User-Id", id.toString());
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Missing user accepted"));
        assertEquals(401, response.getStatus());
    }

    @Test
    void allowsPublicRequestsWithoutCreatingAuthentication() throws Exception {
        var continued = new AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> continued.set(true));
        assertTrue(continued.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
