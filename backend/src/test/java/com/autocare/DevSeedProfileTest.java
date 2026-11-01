package com.autocare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 3 / T3.3: Verification of dev seed data and profile isolation.
 * <p>
 * Ensures that:
 * 1. Default/production profile excludes dev-seed location from Flyway.
 * 2. V2__seed_dev_data.sql exists and contains all required demo entities.
 */
@DisplayName("T3.3 - Dev Seed Profile and Migration Script Verification")
class DevSeedProfileTest {

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Test
    @DisplayName("Default profile only loads standard migrations, excluding dev-seed")
    void defaultProfileExcludesDevSeed() throws Exception {
        Resource defaultYaml = resourceLoader.getResource("classpath:application.yml");
        assertThat(defaultYaml.exists()).isTrue();
        try (InputStream is = defaultYaml.getInputStream()) {
            String yaml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(yaml)
                    .as("Flyway locations in application.yml must NOT contain dev-seed")
                    .doesNotContain("dev-seed");
        }

        Resource devYaml = resourceLoader.getResource("classpath:application-dev.yml");
        assertThat(devYaml.exists()).isTrue();
        try (InputStream is = devYaml.getInputStream()) {
            String yaml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(yaml)
                    .as("Flyway locations in application-dev.yml MUST contain dev-seed")
                    .contains("dev-seed");
        }
    }

    @Test
    @DisplayName("V2__seed_dev_data.sql contains all required demo records satisfying constraints")
    void devSeedFileContainsAllRequiredRecords() throws Exception {
        Resource resource = resourceLoader.getResource("classpath:db/dev-seed/V2__seed_dev_data.sql");
        assertThat(resource.exists())
                .as("V2__seed_dev_data.sql must exist on classpath")
                .isTrue();

        try (InputStream is = resource.getInputStream()) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Customers (at least 3)
            assertThat(sql).contains("Alice Smith");
            assertThat(sql).contains("Bob Johnson");
            assertThat(sql).contains("Charlie Brown");

            // Vehicles (2 Cars, 2 Vans, 2 Motorcycles)
            assertThat(sql).contains("CAB-1001").contains("'CAR'");
            assertThat(sql).contains("CAB-1002").contains("'CAR'");
            assertThat(sql).contains("VAN-2001").contains("'VAN'");
            assertThat(sql).contains("VAN-2002").contains("'VAN'");
            assertThat(sql).contains("MOT-3001").contains("'MOTORCYCLE'");
            assertThat(sql).contains("MOT-3002").contains("'MOTORCYCLE'");

            // Employees (2 Mechanics with different specializations, 1 Manager)
            assertThat(sql).contains("Dave Miller").contains("'MECHANIC'").contains("'Engine'");
            assertThat(sql).contains("Edward White").contains("'MECHANIC'").contains("'Brakes & Suspension'");
            assertThat(sql).contains("Fiona Clark").contains("'MANAGER'");

            // 8 Inventory items (with 2 low-stock)
            assertThat(sql).contains("OIL-5W30");
            assertThat(sql).contains("FLT-OIL01");
            assertThat(sql).contains("BRK-PAD01");
            assertThat(sql).contains("BRK-ROT01");
            assertThat(sql).contains("SPK-IRID");
            assertThat(sql).contains("BAT-12V70");
            assertThat(sql).contains("BLT-TIM01");
            assertThat(sql).contains("WPR-24IN");

            // Completed task and invoice
            assertThat(sql).contains("'COMPLETED'");
            assertThat(sql).contains("'ISSUED'");
            assertThat(sql).contains("task_used_parts");
        }
    }
}
