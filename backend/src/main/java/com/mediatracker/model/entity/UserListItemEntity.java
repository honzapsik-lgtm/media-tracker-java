package com.mediatracker.model.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_list_items")
public class UserListItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "list_id", nullable = false)
    private UUID listId;

    @Column(name = "media_id", nullable = false)
    private String mediaId;

    @Column(name = "media_title")
    private String mediaTitle;

    @Column(name = "media_image")
    private String mediaImage;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getListId() { return listId; }
    public void setListId(UUID listId) { this.listId = listId; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public String getMediaTitle() { return mediaTitle; }
    public void setMediaTitle(String mediaTitle) { this.mediaTitle = mediaTitle; }
    public String getMediaImage() { return mediaImage; }
    public void setMediaImage(String mediaImage) { this.mediaImage = mediaImage; }
    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }
}
