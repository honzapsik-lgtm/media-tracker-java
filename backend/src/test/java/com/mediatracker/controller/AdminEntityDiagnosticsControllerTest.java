package com.mediatracker.controller;

import com.mediatracker.service.AdminDiagnosticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEntityDiagnosticsControllerTest {
    @Mock AdminDiagnosticsService service;
    @InjectMocks AdminController controller;

    @Test
    void userLookupPreservesResponseAndNotFoundContract() {
        var data = Map.<String, Object>of("user", Map.of("id", "user"));
        when(service.userDiagnostics("alice")).thenReturn(Optional.of(data));
        when(service.userDiagnostics("missing")).thenReturn(Optional.empty());
        var found = controller.getUserDiagnostics("alice");
        assertEquals(200, found.getStatusCode().value());
        assertSame(data, found.getBody());
        var missing = controller.getUserDiagnostics("missing");
        assertEquals(404, missing.getStatusCode().value());
        assertEquals(Map.of("error", "User not found"), missing.getBody());
    }

    @Test
    void mediaLookupReturnsServiceDiagnosticsUnchanged() {
        var data = Map.<String, Object>of("tracking", Map.of("id", "tmdb-tv-12"));
        when(service.mediaDiagnostics("tmdb-tv-12")).thenReturn(data);
        var response = controller.getMediaDiagnostics("tmdb-tv-12");
        assertEquals(200, response.getStatusCode().value());
        assertSame(data, response.getBody());
    }
}
