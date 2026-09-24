package com.mediatracker.controller;

import com.mediatracker.model.dto.UnifiedProfileDto;
import com.mediatracker.service.PersonProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/person")
public class PersonProfileController {
    private final PersonProfileService service;
    public PersonProfileController(PersonProfileService service) { this.service = service; }

    @GetMapping("/{slug}")
    public ResponseEntity<UnifiedProfileDto> getProfile(@PathVariable String slug) {
        return ResponseEntity.of(service.getProfile(slug));
    }
}
