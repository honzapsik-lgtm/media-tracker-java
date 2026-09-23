package com.mediatracker.service;

import com.mediatracker.model.entity.UserListEntity;
import com.mediatracker.model.entity.UserListItemEntity;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.repository.UserListItemRepository;
import com.mediatracker.repository.UserListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ListService {

    private final UserListRepository userListRepository;
    private final UserListItemRepository userListItemRepository;

    public ListService(UserListRepository userListRepository, UserListItemRepository userListItemRepository) {
        this.userListRepository = userListRepository;
        this.userListItemRepository = userListItemRepository;
    }

    public List<Map<String, Object>> getUserLists(UUID userId, MediaType mediaType) {
        List<UserListEntity> lists = mediaType != null
                ? userListRepository.findByUserIdAndMediaType(userId, mediaType)
                : userListRepository.findByUserId(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserListEntity l : lists) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("user_id", l.getUserId());
            map.put("title", l.getTitle());
            map.put("media_type", l.getMediaType());
            map.put("created_at", l.getCreatedAt());
            map.put("updated_at", l.getUpdatedAt());
            map.put("_count", Map.of("items", userListItemRepository.countByListId(l.getId())));
            result.add(map);
        }
        return result;
    }

    public Optional<UserListEntity> getListById(UUID listId) {
        return userListRepository.findById(listId);
    }

    public List<UserListItemEntity> getListItems(UUID listId) {
        return userListItemRepository.findByListIdOrderByRankPositionAsc(listId);
    }

    @Transactional
    public UserListEntity createList(UUID userId, String title, MediaType mediaType) {
        UserListEntity list = new UserListEntity();
        list.setUserId(userId);
        list.setTitle(title);
        list.setMediaType(mediaType);
        list.setCreatedAt(OffsetDateTime.now());
        list.setUpdatedAt(OffsetDateTime.now());
        return userListRepository.save(list);
    }

    @Transactional
    public boolean updateListItems(UUID listId, UUID userId, List<Map<String, Object>> mediaItems) {
        Optional<UserListEntity> listOpt = userListRepository.findById(listId);
        if (listOpt.isEmpty()) return false;
        UserListEntity list = listOpt.get();
        if (!list.getUserId().equals(userId)) return false;

        userListItemRepository.deleteByListId(listId);

        if (mediaItems != null && !mediaItems.isEmpty()) {
            List<UserListItemEntity> toSave = new ArrayList<>();
            for (int i = 0; i < mediaItems.size(); i++) {
                Map<String, Object> item = mediaItems.get(i);
                String mediaId = String.valueOf(item.getOrDefault("id", item.get("mediaId")));
                String title = item.containsKey("title") && item.get("title") != null ? String.valueOf(item.get("title")) : null;
                String image = item.containsKey("image") && item.get("image") != null ? String.valueOf(item.get("image")) : null;

                UserListItemEntity li = new UserListItemEntity();
                li.setListId(listId);
                li.setMediaId(mediaId);
                li.setMediaTitle(title);
                li.setMediaImage(image);
                li.setRankPosition(i + 1);
                toSave.add(li);
            }
            userListItemRepository.saveAll(toSave);
        }

        list.setUpdatedAt(OffsetDateTime.now());
        userListRepository.save(list);
        return true;
    }

    @Transactional
    public boolean deleteList(UUID listId, UUID userId) {
        Optional<UserListEntity> listOpt = userListRepository.findById(listId);
        if (listOpt.isEmpty()) return false;
        if (!listOpt.get().getUserId().equals(userId)) return false;

        userListItemRepository.deleteByListId(listId);
        userListRepository.delete(listOpt.get());
        return true;
    }
}
