package com.mediatracker.controller;

import com.mediatracker.model.dto.UnifiedCompanyProfileDto;
import com.mediatracker.service.CompanyProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyProfileController {
    private final CompanyProfileService service;
    public CompanyProfileController(CompanyProfileService service) { this.service = service; }

    @GetMapping("/{slug}")
    public ResponseEntity<UnifiedCompanyProfileDto> getProfile(@PathVariable String slug) {
        return ResponseEntity.of(service.getProfile(slug));
    }
}
