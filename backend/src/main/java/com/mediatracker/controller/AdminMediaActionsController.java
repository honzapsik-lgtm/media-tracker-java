package com.mediatracker.controller;

import com.mediatracker.service.AdminMediaActionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMediaActionsController {
    private final AdminMediaActionsService service;

    public AdminMediaActionsController(AdminMediaActionsService service) {
        this.service = service;
    }

    public record ActionRequest(String action) {}

    @PostMapping("/{mediaId}/actions")
    public ResponseEntity<?> act(@PathVariable String mediaId, @RequestBody ActionRequest request) {
        if (!AdminMediaActionsService.isValidMediaId(mediaId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or unsupported media ID"));
        }
        if ("clear-cache".equals(request.action())) {
            return ResponseEntity.ok(Map.of("success", true, "deletedCount", service.clearCache(mediaId)));
        }
        if ("refresh-stats".equals(request.action())) {
            service.refreshStats(mediaId);
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid media action"));
    }
}
