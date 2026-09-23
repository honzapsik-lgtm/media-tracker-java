package com.mediatracker.repository;

import com.mediatracker.model.entity.UserListEntity;
import com.mediatracker.model.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserListRepository extends JpaRepository<UserListEntity, UUID> {
    List<UserListEntity> findByUserId(UUID userId);
    List<UserListEntity> findByUserIdAndMediaType(UUID userId, MediaType mediaType);
}
