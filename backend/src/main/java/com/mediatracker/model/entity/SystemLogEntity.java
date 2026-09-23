package com.mediatracker.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"SystemLog\"")
public class SystemLogEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "level", nullable = false)
    private String level;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "message")
    private String message;

    @Column(name = "\"requestId\"")
    private String requestId;

    @Column(name = "\"userId\"")
    private String userId;

    @Column(name = "\"mediaId\"")
    private String mediaId;

    @Column(name = "\"mediaType\"")
    private String mediaType;

    @Column(name = "\"jobId\"")
    private String jobId;

    @Column(name = "\"durationMs\"")
    private Integer durationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private JsonNode metadata;

    @Column(name = "\"errorName\"")
    private String errorName;

    @Column(name = "\"errorMessage\"")
    private String errorMessage;

    @Column(name = "\"errorStack\"")
    private String errorStack;

    @Column(name = "\"createdAt\"", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    public JsonNode getMetadata() { return metadata; }
    public void setMetadata(JsonNode metadata) { this.metadata = metadata; }
    public String getErrorName() { return errorName; }
    public void setErrorName(String errorName) { this.errorName = errorName; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorStack() { return errorStack; }
    public void setErrorStack(String errorStack) { this.errorStack = errorStack; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
