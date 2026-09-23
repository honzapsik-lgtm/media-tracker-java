package com.mediatracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.model.entity.ApiCacheEntity;
import com.mediatracker.repository.ApiCacheRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class CacheService {

    private final ApiCacheRepository apiCacheRepository;
    private final ObjectMapper objectMapper;

    public CacheService(ApiCacheRepository apiCacheRepository, ObjectMapper objectMapper) {
        this.apiCacheRepository = apiCacheRepository;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String id, Class<T> clazz) {
        try {
            Optional<ApiCacheEntity> cached = apiCacheRepository.findByIdAndExpiresAtAfter(id, LocalDateTime.now());
            if (cached.isPresent()) {
                JsonNode data = cached.get().getData();
                if (data != null && !data.isNull()) {
                    return Optional.ofNullable(objectMapper.treeToValue(data, clazz));
                }
            }
        } catch (Exception e) {
            // Ignore cache read failures gracefully
        }
        return Optional.empty();
    }

    public Optional<JsonNode> getJson(String id) {
        try {
            return apiCacheRepository.findByIdAndExpiresAtAfter(id, LocalDateTime.now())
                    .map(ApiCacheEntity::getData);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void put(String id, String provider, Object data, int ttlSeconds) {
        try {
            JsonNode jsonNode = objectMapper.valueToTree(data);
            ApiCacheEntity entity = new ApiCacheEntity();
            entity.setId(id);
            entity.setProvider(provider);
            entity.setData(jsonNode);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
            apiCacheRepository.save(entity);
        } catch (Exception e) {
            // Ignore cache write failures gracefully
        }
    }

    public <T> T getOrFetch(String id, String provider, int ttlSeconds, Class<T> clazz, Supplier<T> fetcher) {
        Optional<T> cached = get(id, clazz);
        if (cached.isPresent()) {
            return cached.get();
        }
        T fresh = fetcher.get();
        if (fresh != null) {
            put(id, provider, fresh, ttlSeconds);
        }
        return fresh;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanExpiredCache() {
        apiCacheRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}