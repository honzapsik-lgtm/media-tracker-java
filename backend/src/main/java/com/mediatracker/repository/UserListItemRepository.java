package com.mediatracker.repository;

import com.mediatracker.model.entity.UserListItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserListItemRepository extends JpaRepository<UserListItemEntity, UUID> {
    List<UserListItemEntity> findByListIdOrderByRankPositionAsc(UUID listId);
    Optional<UserListItemEntity> findByListIdAndMediaId(UUID listId, String mediaId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserListItemEntity u WHERE u.listId = :listId AND u.mediaId = :mediaId")
    void deleteByListIdAndMediaId(@Param("listId") UUID listId, @Param("mediaId") String mediaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserListItemEntity u WHERE u.listId = :listId")
    void deleteByListId(@Param("listId") UUID listId);
    long countByListId(UUID listId);
}
