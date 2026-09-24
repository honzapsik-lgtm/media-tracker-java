package com.mediatracker.service;

import com.mediatracker.model.entity.*;
import com.mediatracker.model.enums.MediaType;
import com.mediatracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ListService {

    private final UserListRepository userListRepository;
    private final UserListItemRepository userListItemRepository;
    private final UserRatingRepository userRatingRepository;
    private final UserWatchlistRepository userWatchlistRepository;
    private final MediaRepository mediaRepository;

    public ListService(UserListRepository userListRepository,
                       UserListItemRepository userListItemRepository,
                       UserRatingRepository userRatingRepository,
                       UserWatchlistRepository userWatchlistRepository,
                       MediaRepository mediaRepository) {
        this.userListRepository = userListRepository;
        this.userListItemRepository = userListItemRepository;
        this.userRatingRepository = userRatingRepository;
        this.userWatchlistRepository = userWatchlistRepository;
        this.mediaRepository = mediaRepository;
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

    @Transactional(readOnly = true)
    public Map<String, Object> getListDetails(UUID listId) {
        Optional<UserListEntity> listOpt = userListRepository.findById(listId);
        if (listOpt.isEmpty()) return null;
        UserListEntity list = listOpt.get();

        List<UserListItemEntity> items = userListItemRepository.findByListIdOrderByRankPositionAsc(listId);
        List<String> mediaIds = items.stream().map(UserListItemEntity::getMediaId).toList();

        Map<String, UserRatingEntity> ratingsByMedia = new HashMap<>();
        if (!mediaIds.isEmpty()) {
            List<UserRatingEntity> ratings = userRatingRepository.findByUserIdAndMediaIdIn(list.getUserId(), mediaIds);
            for (UserRatingEntity r : ratings) {
                ratingsByMedia.put(r.getMediaId(), r);
            }
        }

        List<Map<String, Object>> formattedItems = new ArrayList<>();
        for (UserListItemEntity item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            UserRatingEntity rating = ratingsByMedia.get(item.getMediaId());

            itemMap.put("id", item.getId());
            itemMap.put("listId", item.getListId());
            itemMap.put("mediaId", item.getMediaId());
            itemMap.put("rankPosition", item.getRankPosition());
            itemMap.put("mediaTitle", item.getMediaTitle());
            itemMap.put("mediaImage", item.getMediaImage());

            String title = item.getMediaTitle();
            if (title == null || title.isBlank() || "Unknown Title".equalsIgnoreCase(title)) {
                if (rating != null && rating.getMediaTitle() != null && !rating.getMediaTitle().isBlank()) {
                    title = rating.getMediaTitle();
                } else {
                    Optional<UserWatchlistEntity> wOpt = userWatchlistRepository.findByUserIdAndMediaId(list.getUserId(), item.getMediaId());
                    if (wOpt.isPresent() && wOpt.get().getMediaTitle() != null && !wOpt.get().getMediaTitle().isBlank()) {
                        title = wOpt.get().getMediaTitle();
                    } else {
                        Optional<MediaEntity> mOpt = mediaRepository.findById(item.getMediaId());
                        if (mOpt.isPresent() && mOpt.get().getTitle() != null && !mOpt.get().getTitle().isBlank()) {
                            title = mOpt.get().getTitle();
                        }
                    }
                }
            }
            if (title == null || title.isBlank()) {
                title = "Unknown Title";
            }

            String image = item.getMediaImage();
            if (image == null || image.isBlank()) {
                if (rating != null && rating.getMediaImage() != null && !rating.getMediaImage().isBlank()) {
                    image = rating.getMediaImage();
                } else {
                    Optional<UserWatchlistEntity> wOpt = userWatchlistRepository.findByUserIdAndMediaId(list.getUserId(), item.getMediaId());
                    if (wOpt.isPresent() && wOpt.get().getMediaImage() != null) {
                        image = wOpt.get().getMediaImage();
                    }
                }
            }

            String releaseDate = rating != null ? rating.getMediaReleaseDate() : null;

            itemMap.put("title", title);
            itemMap.put("image", image);
            itemMap.put("type", list.getMediaType() != null ? list.getMediaType().name().toLowerCase() : "show");
            itemMap.put("score", rating != null && rating.getScore() != null ? rating.getScore() : 0);
            itemMap.put("hasRated", rating != null);
            itemMap.put("reviewText", rating != null ? rating.getReviewText() : null);
            itemMap.put("releaseDate", releaseDate);
            if (rating != null && rating.getCriteriaScores() != null) {
                itemMap.put("criteriaScores", rating.getCriteriaScores());
            }

            formattedItems.add(itemMap);
        }

        Map<String, Object> listMap = new HashMap<>();
        listMap.put("id", list.getId());
        listMap.put("userId", list.getUserId());
        listMap.put("user_id", list.getUserId());
        listMap.put("title", list.getTitle());
        listMap.put("mediaType", list.getMediaType());
        listMap.put("media_type", list.getMediaType() != null ? list.getMediaType().name().toLowerCase() : "show");
        listMap.put("createdAt", list.getCreatedAt());
        listMap.put("updatedAt", list.getUpdatedAt());

        Map<String, Object> result = new HashMap<>(listMap);
        result.put("list", listMap);
        result.put("items", formattedItems);
        return result;
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
                String title = item.get("title") != null ? String.valueOf(item.get("title"))
                        : item.get("mediaTitle") != null ? String.valueOf(item.get("mediaTitle")) : null;
                String image = item.get("image") != null ? String.valueOf(item.get("image"))
                        : item.get("mediaImage") != null ? String.valueOf(item.get("mediaImage")) : null;

                // Resolve missing titles or images from user ratings, watchlist, or media catalog
                if (title == null || title.isBlank() || "null".equals(title) || "Unknown Title".equalsIgnoreCase(title)) {
                    Optional<UserRatingEntity> rOpt = userRatingRepository.findByUserIdAndMediaId(userId, mediaId);
                    if (rOpt.isPresent() && rOpt.get().getMediaTitle() != null && !rOpt.get().getMediaTitle().isBlank()) {
                        title = rOpt.get().getMediaTitle();
                        if (image == null || image.isBlank() || "null".equals(image)) {
                            image = rOpt.get().getMediaImage();
                        }
                    } else {
                        Optional<UserWatchlistEntity> wOpt = userWatchlistRepository.findByUserIdAndMediaId(userId, mediaId);
                        if (wOpt.isPresent() && wOpt.get().getMediaTitle() != null && !wOpt.get().getMediaTitle().isBlank()) {
                            title = wOpt.get().getMediaTitle();
                            if (image == null || image.isBlank() || "null".equals(image)) {
                                image = wOpt.get().getMediaImage();
                            }
                        } else {
                            Optional<MediaEntity> mOpt = mediaRepository.findById(mediaId);
                            if (mOpt.isPresent() && mOpt.get().getTitle() != null && !mOpt.get().getTitle().isBlank()) {
                                title = mOpt.get().getTitle();
                            }
                        }
                    }
                }

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
