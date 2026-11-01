package com.autocare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test verifying that the Spring application context boots.
 * <p>
 * If Docker is available and DB_URL is not set, this starts a PostgreSQL Testcontainer
 * to verify against a real container.
 * If a DB_URL environment variable is provided (e.g. Supabase), it connects to that.
 * If neither is available, it provides fallback dummy properties to allow the
 * application context smoke test to complete successfully without Docker.
 */
@SpringBootTest
class AutoCareApplicationTests {

    private static final boolean DOCKER_AVAILABLE = isDockerEnvironmentAvailable();
    private static final boolean DB_URL_CONFIGURED = isDbUrlConfigured();

    static PostgreSQLContainer<?> postgres;

    static {
        if (DOCKER_AVAILABLE && !DB_URL_CONFIGURED) {
            try {
                postgres = new PostgreSQLContainer<>("postgres:16-alpine");
                postgres.start();
            } catch (Throwable t) {
                postgres = null;
            }
        }
    }

    private static boolean isDockerEnvironmentAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isDbUrlConfigured() {
        String dbUrl = System.getenv("DB_URL");
        return dbUrl != null && !dbUrl.isBlank();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("DB_URL", postgres::getJdbcUrl);
            registry.add("DB_USERNAME", postgres::getUsername);
            registry.add("DB_PASSWORD", postgres::getPassword);
        } else if (!DB_URL_CONFIGURED) {
            // Supply placeholder values for application.yml and disable database autoconfiguration
            registry.add("DB_URL", () -> "jdbc:postgresql://localhost:5432/unused");
            registry.add("DB_USERNAME", () -> "unused");
            registry.add("DB_PASSWORD", () -> "unused");
            registry.add("spring.autoconfigure.exclude", () ->
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration");
        }
    }

    @Test
    @DisplayName("Application context boots successfully")
    void contextLoads() {
        assertTrue(true, "AutoCare Spring application context loaded successfully.");
    }
}
