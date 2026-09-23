package com.mediatracker.model.entity;

import com.mediatracker.model.enums.MediaType;
import com.mediatracker.model.enums.WatchlistStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_watchlist")
public class UserWatchlistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "media_id", nullable = false)
    private String mediaId;

    @Column(name = "media_title")
    private String mediaTitle;

    @Column(name = "media_image")
    private String mediaImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WatchlistStatus status = WatchlistStatus.PLANNING;

    @Column(name = "added_at")
    private OffsetDateTime addedAt = OffsetDateTime.now();

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "\"episodesWatched\"", nullable = false)
    private Integer episodesWatched = 0;

    @Column(name = "is_rewatching", nullable = false)
    private boolean isRewatching = false;

    @Column(name = "\"chaptersRead\"", nullable = false)
    private Integer chaptersRead = 0;

    @Column(name = "\"volumesRead\"", nullable = false)
    private Integer volumesRead = 0;

    @Column(name = "is_rereading", nullable = false)
    private boolean isRereading = false;

    @Column(name = "\"hoursPlayed\"", nullable = false)
    private Double hoursPlayed = 0.0;

    @Column(name = "platform")
    private String platform;

    @Column(name = "\"watchCount\"", nullable = false)
    private Integer watchCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public String getMediaTitle() { return mediaTitle; }
    public void setMediaTitle(String mediaTitle) { this.mediaTitle = mediaTitle; }
    public String getMediaImage() { return mediaImage; }
    public void setMediaImage(String mediaImage) { this.mediaImage = mediaImage; }
    public WatchlistStatus getStatus() { return status; }
    public void setStatus(WatchlistStatus status) { this.status = status; }
    public OffsetDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(OffsetDateTime addedAt) { this.addedAt = addedAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Integer getEpisodesWatched() { return episodesWatched; }
    public void setEpisodesWatched(Integer episodesWatched) { this.episodesWatched = episodesWatched; }
    public boolean isRewatching() { return isRewatching; }
    public void setRewatching(boolean rewatching) { isRewatching = rewatching; }
    public Integer getChaptersRead() { return chaptersRead; }
    public void setChaptersRead(Integer chaptersRead) { this.chaptersRead = chaptersRead; }
    public Integer getVolumesRead() { return volumesRead; }
    public void setVolumesRead(Integer volumesRead) { this.volumesRead = volumesRead; }
    public boolean isRereading() { return isRereading; }
    public void setRereading(boolean rereading) { isRereading = rereading; }
    public Double getHoursPlayed() { return hoursPlayed; }
    public void setHoursPlayed(Double hoursPlayed) { this.hoursPlayed = hoursPlayed; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Integer getWatchCount() { return watchCount; }
    public void setWatchCount(Integer watchCount) { this.watchCount = watchCount; }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
}
