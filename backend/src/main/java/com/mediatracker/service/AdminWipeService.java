package com.mediatracker.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminWipeService {

    private static final Logger log = LoggerFactory.getLogger(AdminWipeService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void wipeAppData() {
        log.warn("Starting FULL application data wipe (preserving auth and users)...");

        String sql = """
            DELETE FROM user_activities;
            DELETE FROM user_friend_preferences;
            DELETE FROM friendships;
            DELETE FROM user_privacy_settings;
            DELETE FROM activity_log;
            DELETE FROM episodes;
            DELETE FROM seasons;
            DELETE FROM media;
            DELETE FROM user_ratings;
            DELETE FROM user_watchlist;
            DELETE FROM media_stats;
            DELETE FROM global_rankings;
            DELETE FROM user_list_items;
            DELETE FROM user_lists;
            DELETE FROM "BackgroundJob";
            DELETE FROM user_stats_cache;
            DELETE FROM "ApiCache";
            DELETE FROM "SystemLog";
            DELETE FROM user_badges;
            DELETE FROM people;
            DELETE FROM companies;
            UPDATE users SET "showcaseBadges" = ARRAY[]::text[];
        """;

        entityManager.createNativeQuery(sql).executeUpdate();
        log.warn("FULL application data wipe completed successfully.");
    }
}
