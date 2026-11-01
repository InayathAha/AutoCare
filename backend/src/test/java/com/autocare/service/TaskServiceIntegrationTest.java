package com.autocare.service;

import com.autocare.domain.*;
import com.autocare.dto.TaskPartRequest;
import com.autocare.exception.InsufficientStockException;
import com.autocare.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Milestone 4 / T4.1 Acceptance Test:
 * Verifies that TaskService.addPart runs in a single transactional unit of work:
 * stock deduction and task used part attachment succeed together or roll back on failure.
 */
@SpringBootTest
@DisplayName("T4.1 - TaskService Transactional Stock Rollback Acceptance Test")
class TaskServiceIntegrationTest {

    private static final Map<String, String> DOTENV = loadDotenv();

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean isDbConfigured() {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null && !dbUrl.isBlank()) {
            return true;
        }
        return DOTENV.containsKey("DB_URL") && !DOTENV.get("DB_URL").isBlank();
    }

    static PostgreSQLContainer<?> postgres;

    static {
        if (isDockerAvailable() && !isDbConfigured()) {
            try {
                postgres = new PostgreSQLContainer<>("postgres:16-alpine");
                postgres.start();
            } catch (Throwable t) {
                postgres = null;
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("DB_URL", postgres::getJdbcUrl);
            registry.add("DB_USERNAME", postgres::getUsername);
            registry.add("DB_PASSWORD", postgres::getPassword);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
        } else if (isDbConfigured()) {
            String url = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : DOTENV.get("DB_URL");
            String user = System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : DOTENV.get("DB_USERNAME");
            String pass = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : DOTENV.get("DB_PASSWORD");

            registry.add("DB_URL", () -> url);
            registry.add("DB_USERNAME", () -> user != null ? user : "postgres");
            registry.add("DB_PASSWORD", () -> pass != null ? pass : "");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
        } else {
            registry.add("DB_URL", () -> "jdbc:postgresql://localhost:5432/unused");
            registry.add("DB_USERNAME", () -> "unused");
            registry.add("DB_PASSWORD", () -> "unused");
            registry.add("spring.autoconfigure.exclude", () ->
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration");
        }
    }

    static boolean isDbAvailable() {
        return (postgres != null && postgres.isRunning()) || isDbConfigured();
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> map = new HashMap<>();
        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("../.env"),
                Path.of("../../.env"),
                Path.of("backend/.env")
        );

        for (Path p : candidates) {
            if (Files.exists(p)) {
                try {
                    for (String line : Files.readAllLines(p)) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                        int idx = trimmed.indexOf('=');
                        if (idx > 0) {
                            String k = trimmed.substring(0, idx).trim();
                            String v = trimmed.substring(idx + 1).trim();
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length() - 1);
                            }
                            map.putIfAbsent(k, v);
                        }
                    }
                    break;
                } catch (IOException ignored) {
                }
            }
        }
        return map;
    }

    @Autowired(required = false)
    private TaskService taskService;

    @Autowired(required = false)
    private CustomerRepository customerRepository;

    @Autowired(required = false)
    private VehicleRepository vehicleRepository;

    @Autowired(required = false)
    private EmployeeRepository employeeRepository;

    @Autowired(required = false)
    private InventoryItemRepository inventoryItemRepository;

    @Autowired(required = false)
    private InvoiceRepository invoiceRepository;

    @Autowired(required = false)
    private ServiceTaskRepository serviceTaskRepository;

    @Test
    @DisplayName("TaskService.addPart rolls back inventory stock and task state on failure")
    @EnabledIf("isDbAvailable")
    @org.springframework.transaction.annotation.Transactional
    void addPart_rollsBackStockOnFailure() {
        String testSuffix = UUID.randomUUID().toString().substring(0, 8);
        String custId = "cust-tx-" + testSuffix;
        String plate = "TX-" + testSuffix.toUpperCase();
        String mechId = "mech-tx-" + testSuffix;
        String partCode = "TX-PART-" + testSuffix.toUpperCase();
        String invId = "inv-tx-" + testSuffix;
        String taskId = "task-tx-" + testSuffix;

        Customer customer = new Customer(custId, "TX Customer", "0771234567", "tx-" + testSuffix + "@test.com");
        customerRepository.save(customer);

        Car car = new Car(plate, "Toyota", "Corolla", 5);
        customer.registerVehicle(car);
        vehicleRepository.save(car);

        Mechanic mechanic = new Mechanic(mechId, "TX Mechanic", "0777654321", "mech-" + testSuffix + "@test.com", 1000.0, "Engine", 20.0);
        employeeRepository.save(mechanic);

        // Inventory item with initial stock = 3
        InventoryItem item = new InventoryItem(partCode, "TX Part", 15.00, 3, 1);
        inventoryItemRepository.save(item);

        Invoice invoice = new Invoice(invId, customer, car);
        ServiceTask task = new ServiceTask(taskId, "Test TX Task", mechanic);
        invoice.addTask(task);
        invoiceRepository.save(invoice);

        // Attempt to consume 5 units (stock is only 3) -> throws InsufficientStockException
        TaskPartRequest overRequest = new TaskPartRequest(partCode, 5);
        assertThatThrownBy(() -> taskService.addPart(taskId, overRequest))
                .isInstanceOf(InsufficientStockException.class);

        // Verify stock is untouched and remains 3 in database
        InventoryItem afterItem = inventoryItemRepository.findById(partCode).orElseThrow();
        assertThat(afterItem.getStockQuantity()).isEqualTo(3);

        // Verify task has no used parts attached
        ServiceTask afterTask = serviceTaskRepository.findById(taskId).orElseThrow();
        assertThat(afterTask.getUsedParts()).isEmpty();
        assertThat(afterTask.getPartsCost()).isEqualTo(0.0);
    }
}
