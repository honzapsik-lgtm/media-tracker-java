package com.mediatracker.controller;

import com.mediatracker.model.entity.UserListEntity;
import com.mediatracker.model.entity.UserListItemEntity;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.security.SecurityUtils;
import com.mediatracker.service.ListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/lists")
@Tag(name = "Custom Lists", description = "Endpoints for managing custom user ranking lists and item ordering")
public class CustomListController {

    private final ListService listService;

    public CustomListController(ListService listService) {
        this.listService = listService;
    }

    @GetMapping
    @Operation(summary = "Get user lists, optionally filtered by media type")
    public ResponseEntity<?> getLists(@RequestParam(required = false) String media_type) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();

        MediaType mType = null;
        if (media_type != null && !media_type.isBlank()) {
            try { mType = MediaType.valueOf(media_type.trim().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        List<Map<String, Object>> lists = listService.getUserLists(currentUserId, mType);
        return ResponseEntity.ok(Map.of("lists", lists));
    }

    public record CreateListRequest(String title, String media_type) {}

    @PostMapping
    @Operation(summary = "Create a new custom list")
    public ResponseEntity<?> createList(@RequestBody CreateListRequest req) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();

        if (req.title() == null || req.title().isBlank() || req.media_type() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "title and media_type are required"));
        }

        MediaType mType;
        try {
            mType = MediaType.valueOf(req.media_type().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid media_type"));
        }

        UserListEntity created = listService.createList(currentUserId, req.title(), mType);
        return ResponseEntity.status(201).body(Map.of("list", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get custom list with items by id")
    public ResponseEntity<?> getListById(@PathVariable UUID id) {
        return listService.getListById(id).map(list -> {
            List<UserListItemEntity> items = listService.getListItems(id);
            Map<String, Object> map = new HashMap<>();
            map.put("id", list.getId());
            map.put("userId", list.getUserId());
            map.put("title", list.getTitle());
            map.put("mediaType", list.getMediaType());
            map.put("items", items);
            map.put("createdAt", list.getCreatedAt());
            map.put("updatedAt", list.getUpdatedAt());
            return ResponseEntity.ok((Object) map);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record UpdateItemsRequest(List<Map<String, Object>> mediaIds) {}

    @PostMapping("/{id}/items")
    @Operation(summary = "Reorder and save items in custom list")
    public ResponseEntity<?> updateListItems(@PathVariable UUID id, @RequestBody UpdateItemsRequest req) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        boolean updated = listService.updateListItems(id, currentUserId, req.mediaIds());
        if (!updated) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update list items"));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom list")
    public ResponseEntity<?> deleteList(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        boolean deleted = listService.deleteList(id, currentUserId);
        return ResponseEntity.ok(Map.of("success", deleted));
    }
}
