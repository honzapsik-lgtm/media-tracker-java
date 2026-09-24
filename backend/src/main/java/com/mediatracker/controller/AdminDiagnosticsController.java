package com.mediatracker.controller;

import com.mediatracker.service.AdminDiagnosticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/diagnostics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiagnosticsController {
    private final AdminDiagnosticsService service;

    public AdminDiagnosticsController(AdminDiagnosticsService service) {
        this.service = service;
    }

    @GetMapping("/database/summary")
    public Object database() { return service.databaseSummary(); }

    @GetMapping("/database/integrity")
    public Object integrity() { return service.integrityChecks(); }

    @GetMapping("/logs/summary")
    public Object logSummary() { return service.logSummary(); }

    @GetMapping("/logs")
    public Object logs(@RequestParam Map<String, String> filters) { return service.logs(filters, false); }

    @GetMapping("/cache/summary")
    public Object cacheSummary() { return service.cacheSummary(); }

    @GetMapping("/cache")
    public Object cache(@RequestParam Map<String, String> filters) { return service.cache(filters); }

    @GetMapping("/performance/summary")
    public Object performanceSummary() { return service.performanceSummary(); }

    @GetMapping("/performance")
    public Object performance(@RequestParam Map<String, String> filters) {
        var result = service.logs(filters, true);
        result.put("summary", service.performanceSummary());
        return result;
    }
}
