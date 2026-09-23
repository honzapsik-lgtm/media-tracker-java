package com.mediatracker.repository;

import com.mediatracker.model.entity.UserListItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserListItemRepository extends JpaRepository<UserListItemEntity, UUID> {
    List<UserListItemEntity> findByListIdOrderByRankPositionAsc(UUID listId);
    Optional<UserListItemEntity> findByListIdAndMediaId(UUID listId, String mediaId);
    void deleteByListIdAndMediaId(UUID listId, String mediaId);
    void deleteByListId(UUID listId);
    long countByListId(UUID listId);
}
