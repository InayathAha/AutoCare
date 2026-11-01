package com.autocare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 3 / T3.1: Flyway Schema Migration and Hibernate Validation Test.
 * <p>
 * Verifies that:
 * 1. V1__init_schema.sql migrates cleanly on PostgreSQL.
 * 2. Spring Boot JPA context boots with ddl-auto=validate against the migrated schema.
 * 3. Row Level Security (RLS) is enabled on all 7 application tables.
 * <p>
 * When Docker is available, this spins up a fresh PostgreSQL Testcontainer.
 * If Docker is unavailable in the environment, the migration SQL structure is verified statically.
 */
@SpringBootTest
@DisplayName("T3.1 - Flyway Schema & PostgreSQL Persistence Acceptance")
class SchemaMigrationTest {

    private static final String MIGRATION_PATH = "db/migration/V1__init_schema.sql";

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "customers",
            "vehicles",
            "employees",
            "inventory_items",
            "invoices",
            "service_tasks",
            "task_used_parts"
    );

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean isDbConfigured() {
        String dbUrl = System.getenv("DB_URL");
        return dbUrl != null && !dbUrl.isBlank();
    }

    @Container
    static PostgreSQLContainer<?> postgres = isDockerAvailable() && !isDbConfigured()
            ? new PostgreSQLContainer<>("postgres:16-alpine")
            : null;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("DB_URL", postgres::getJdbcUrl);
            registry.add("DB_USERNAME", postgres::getUsername);
            registry.add("DB_PASSWORD", postgres::getPassword);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
        } else if (isDbConfigured()) {
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
        } else {
            // Provide placeholder values for offline testing without Docker
            registry.add("DB_URL", () -> "jdbc:postgresql://localhost:5432/unused");
            registry.add("DB_USERNAME", () -> "unused");
            registry.add("DB_PASSWORD", () -> "unused");
            registry.add("spring.autoconfigure.exclude", () ->
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration");
        }
    }

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V1__init_schema.sql file exists on classpath and contains all 7 tables with RLS")
    void migrationScriptVerifiesContent() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(MIGRATION_PATH)) {
            assertThat(in)
                    .as("Flyway migration script %s must be present on classpath", MIGRATION_PATH)
                    .isNotNull();

            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String normalizedSql = sql.replaceAll("\\s+", " ");

            for (String table : EXPECTED_TABLES) {
                assertThat(normalizedSql)
                        .as("Migration SQL must create table %s", table)
                        .containsIgnoringCase("CREATE TABLE " + table);
                assertThat(normalizedSql)
                        .as("Migration SQL must enable Row Level Security on %s", table)
                        .containsIgnoringCase("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
            }

            // Verify specific constraints from 03-database-schema.md
            assertThat(sql).containsIgnoringCase("ON DELETE RESTRICT");
            assertThat(sql).containsIgnoringCase("uq_task_part UNIQUE (task_id, part_code)");
            assertThat(sql).containsIgnoringCase("vehicles_type_columns");
            assertThat(sql).containsIgnoringCase("employees_role_columns");
        }
    }

    @Test
    @EnabledIf("isDockerOrDbAvailable")
    @DisplayName("Flyway migration applied and RLS enabled on all tables in PostgreSQL")
    void postgresMigrationAndRowLevelSecurityVerified() {
        assertThat(jdbcTemplate).isNotNull();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public'"
        );

        Map<String, Boolean> tableRlsMap = rows.stream().collect(
                java.util.stream.Collectors.toMap(
                        r -> ((String) r.get("tablename")).toLowerCase(),
                        r -> (Boolean) r.get("rowsecurity")
                )
        );

        for (String expectedTable : EXPECTED_TABLES) {
            assertThat(tableRlsMap)
                    .as("Table %s must exist in public schema", expectedTable)
                    .containsKey(expectedTable);

            assertThat(tableRlsMap.get(expectedTable))
                    .as("Table %s must have Row Level Security enabled (rowsecurity=true)", expectedTable)
                    .isTrue();
        }
    }

    static boolean isDockerOrDbAvailable() {
        return isDockerAvailable() || isDbConfigured();
    }
}
