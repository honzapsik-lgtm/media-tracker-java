package com.mediatracker.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class AdminJobFilteringTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static LocalContainerEntityManagerFactoryBean factory;
    static EntityManager em;
    static BackgroundJobRepository jobs;

    @BeforeAll
    static void setup() {
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__init_schema.sql")).execute(source);
        var jdbc = new JdbcTemplate(source);
        jdbc.update("""
            INSERT INTO "BackgroundJob" (id, type, status, payload, dedupe_key, last_error, created_at, updated_at)
            VALUES ('a', 'award_badges', 'pending', '{"userId":"u1","note":"needle"}', 'badge:100%', 'timeout', '2026-01-01', CURRENT_TIMESTAMP),
                   ('b', 'award_badges', 'failed', '{"userId":"u2"}', 'badge:1000', 'timeout', '2026-01-01', CURRENT_TIMESTAMP),
                   ('c', 'update_user_stats', 'pending', '{"userId":"u10"}', NULL, NULL, '2026-01-01', CURRENT_TIMESTAMP),
                   ('d', 'award_badges', 'pending', '{"userId":"u1"}', 'badge:100%:d', NULL, '2026-01-01', CURRENT_TIMESTAMP)
            """);
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(source);
        factory.setPackagesToScan("com.mediatracker.model.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
        em = factory.getObject().createEntityManager();
        jobs = new JpaRepositoryFactory(em).getRepository(BackgroundJobRepository.class);
    }

    @AfterAll
    static void close() {
        if (em != null) em.close();
        if (factory != null) factory.destroy();
    }

    @Test
    void combinedFiltersAndCountUseSamePredicate() {
        var page = jobs.findAdminJobs("pending", "award_badges", "BADGE:", "u1", null, PageRequest.of(0, 1));
        assertEquals(2, page.getTotalElements());
        assertEquals("d", page.getContent().getFirst().getId());
        var second = jobs.findAdminJobs("pending", "award_badges", "badge:", "u1", null, PageRequest.of(1, 1));
        assertEquals(2, second.getTotalElements());
        assertEquals("a", second.getContent().getFirst().getId());
        assertEquals(1, jobs.findAdminJobs("pending", "award_badges", "badge:", "u1", "TIMEOUT", PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void userIdIsExactAndTypeFilterIsIndependent() {
        assertEquals(2, jobs.findAdminJobs(null, null, null, "u1", null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals("c", jobs.findAdminJobs(null, "update_user_stats", null, null, null, PageRequest.of(0, 10))
                .getContent().getFirst().getId());
    }

    @Test
    void substringSearchIsLiteralAndCoversPayloadAndErrors() {
        for (String term : List.of("needle", "100%")) {
            assertFalse(jobs.findAdminJobs(null, null, null, null, term, PageRequest.of(0, 10)).isEmpty());
        }
        assertEquals(2, jobs.findAdminJobs(null, null, "%", null, null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, jobs.findAdminJobs(null, null, null, null, "' OR 1=1 --", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, jobs.findAdminJobs(null, null, "_", null, null, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void noFiltersAndOutOfRangePageRetainAccurateTotals() {
        assertEquals(4, jobs.findAdminJobs(null, null, null, null, null, PageRequest.of(0, 2)).getTotalElements());
        var page = jobs.findAdminJobs(null, null, null, null, null, PageRequest.of(5, 2));
        assertTrue(page.isEmpty());
        assertEquals(4, page.getTotalElements());
    }
}
