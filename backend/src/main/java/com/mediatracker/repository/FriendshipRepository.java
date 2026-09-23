package com.mediatracker.repository;

import com.mediatracker.model.entity.FriendshipEntity;
import com.mediatracker.model.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<FriendshipEntity, UUID> {
    Optional<FriendshipEntity> findBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    @Query("SELECT f FROM FriendshipEntity f WHERE (f.senderId = :userId OR f.receiverId = :userId) AND f.status = :status")
    List<FriendshipEntity> findUserFriendships(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);

    default List<FriendshipEntity> findAcceptedFriendships(UUID userId) {
        return findUserFriendships(userId, FriendshipStatus.ACCEPTED);
    }

    List<FriendshipEntity> findByReceiverIdAndStatus(UUID receiverId, FriendshipStatus status);
    List<FriendshipEntity> findBySenderIdAndStatus(UUID senderId, FriendshipStatus status);

    @Query("SELECT f FROM FriendshipEntity f WHERE (f.senderId = :u1 AND f.receiverId = :u2) OR (f.senderId = :u2 AND f.receiverId = :u1)")
    Optional<FriendshipEntity> findBetweenUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
