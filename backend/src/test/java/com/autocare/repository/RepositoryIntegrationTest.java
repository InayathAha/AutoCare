package com.autocare.repository;

import com.autocare.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Milestone 3 / T3.2: Persistence Layer and Repository Integration Tests.
 * <p>
 * Verifies all 5 acceptance checks specified in docs/03-database-schema.md §7:
 * 1. Polymorphic load check (concrete Car, Van, Motorcycle instances loaded from DB).
 * 2. Low-stock query returns items with stockQuantity <= reorderThreshold.
 * 3. FK violation occurs when attempting to delete a customer who has registered vehicles.
 * 4. Snapshot price independence (modifying inventory unit price does not alter existing task used part snapshot).
 * 5. CHECK constraint violation when attempting to set stock_quantity negative in PostgreSQL.
 */
@SpringBootTest
@DisplayName("T3.2 - Repository & Persistence Acceptance Suite")
class RepositoryIntegrationTest {

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
                    List<String> lines = Files.readAllLines(p);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.startsWith("#") && trimmed.contains("=")) {
                            int idx = trimmed.indexOf('=');
                            map.put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim());
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
    private CustomerRepository customerRepository;

    @Autowired(required = false)
    private VehicleRepository vehicleRepository;

    @Autowired(required = false)
    private EmployeeRepository employeeRepository;

    @Autowired(required = false)
    private InventoryItemRepository inventoryItemRepository;

    @Autowired(required = false)
    private ServiceTaskRepository serviceTaskRepository;

    @Autowired(required = false)
    private InvoiceRepository invoiceRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private EntityManager entityManager;

    @Test
    @EnabledIf("isDbAvailable")
    @Transactional
    @DisplayName("Check 1: Polymorphic load returns concrete Car, Van, and Motorcycle instances")
    void check1_polymorphicVehicleLoad() {
        String uid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Customer customer = new Customer(UUID.randomUUID().toString(), "Poly Owner " + uid, "077000" + uid.substring(0, 4), "poly_" + uid + "@garage.com");
        Customer savedCustomer = customerRepository.save(customer);

        String carPlate = "CAR-" + uid;
        String vanPlate = "VAN-" + uid;
        String motoPlate = "MOT-" + uid;

        Car car = new Car(carPlate, "Toyota", "Corolla", savedCustomer, 5);
        Van van = new Van(vanPlate, "Ford", "Transit", savedCustomer, 12.5);
        Motorcycle moto = new Motorcycle(motoPlate, "Yamaha", "MT-07", savedCustomer, 689);

        savedCustomer.registerVehicle(car);
        savedCustomer.registerVehicle(van);
        savedCustomer.registerVehicle(moto);

        customerRepository.save(savedCustomer);
        entityManager.flush();
        entityManager.clear();

        // Query by ID and assert concrete polymorphic instances
        Vehicle loadedCar = vehicleRepository.findById(carPlate).orElseThrow();
        assertThat(loadedCar).isInstanceOf(Car.class);
        assertThat(loadedCar.getBaseServiceFee()).isEqualTo(50.0);
        assertThat(((Car) loadedCar).getSeatingCapacity()).isEqualTo(5);
        assertThat(loadedCar.getVehicleType()).isEqualTo("Car");

        Vehicle loadedVan = vehicleRepository.findById(vanPlate).orElseThrow();
        assertThat(loadedVan).isInstanceOf(Van.class);
        assertThat(loadedVan.getBaseServiceFee()).isEqualTo(85.0);
        assertThat(((Van) loadedVan).getCargoVolume()).isEqualTo(12.5);
        assertThat(loadedVan.getVehicleType()).isEqualTo("Van");

        Vehicle loadedMoto = vehicleRepository.findById(motoPlate).orElseThrow();
        assertThat(loadedMoto).isInstanceOf(Motorcycle.class);
        assertThat(loadedMoto.getBaseServiceFee()).isEqualTo(30.0);
        assertThat(((Motorcycle) loadedMoto).getEngineCC()).isEqualTo(689);
        assertThat(loadedMoto.getVehicleType()).isEqualTo("Motorcycle");

        // Query customer vehicles through findByOwnerId
        List<Vehicle> customerVehicles = vehicleRepository.findByOwnerId(savedCustomer.getId());
        assertThat(customerVehicles).hasSize(3);
    }

    @Test
    @EnabledIf("isDbAvailable")
    @Transactional
    @DisplayName("Check 2: Low-stock query correctly retrieves items with stockQuantity <= reorderThreshold")
    void check2_lowStockQuery() {
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String lowPartCode = "LOW-" + uid;
        String okPartCode = "OK-" + uid;

        InventoryItem lowItem = new InventoryItem(lowPartCode, "Low Stock Filter " + uid, 25.00, 4, 10);
        InventoryItem okItem = new InventoryItem(okPartCode, "Adequate Stock Oil " + uid, 40.00, 25, 10);

        inventoryItemRepository.saveAll(List.of(lowItem, okItem));
        entityManager.flush();
        entityManager.clear();

        List<InventoryItem> lowStockList = inventoryItemRepository.findLowStock();
        assertThat(lowStockList)
                .extracting(InventoryItem::getPartCode)
                .contains(lowPartCode)
                .doesNotContain(okPartCode);
    }

    @Test
    @EnabledIf("isDbAvailable")
    @Transactional
    @DisplayName("Check 3: Foreign Key violation (RESTRICT) when deleting a customer with vehicles")
    void check3_fkViolationOnDeletingCustomerWithVehicles() {
        String uid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Customer customer = new Customer(UUID.randomUUID().toString(), "FK Test " + uid, "077111" + uid.substring(0, 4), "fk_" + uid + "@garage.com");
        Customer savedCustomer = customerRepository.save(customer);

        Car car = new Car("FK-" + uid, "Honda", "Civic", savedCustomer, 5);
        savedCustomer.registerVehicle(car);
        customerRepository.save(savedCustomer);
        entityManager.flush();

        // Direct SQL delete on customer while vehicle exists must be rejected by PostgreSQL FK ON DELETE RESTRICT
        assertThatThrownBy(() -> {
            jdbcTemplate.update("DELETE FROM customers WHERE customer_id = ?", savedCustomer.getId());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @EnabledIf("isDbAvailable")
    @Transactional
    @DisplayName("Check 4: Attaching a part snapshots unit_price; subsequent inventory item price changes do not alter snapshot")
    void check4_snapshotPriceIndependence() {
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Customer customer = customerRepository.save(new Customer(UUID.randomUUID().toString(), "Snap Cust " + uid, "077222" + uid, "snap_" + uid + "@garage.com"));
        Car car = new Car("SNAP-" + uid, "Mazda", "3", customer, 5);
        customer.registerVehicle(car);
        customerRepository.save(customer);

        Mechanic mechanic = employeeRepository.save(new Mechanic(UUID.randomUUID().toString(), "Snap Mech " + uid, "077333" + uid, "mech_" + uid + "@garage.com", 2500.0, "Brakes", 25.0));

        InventoryItem item = inventoryItemRepository.save(new InventoryItem("PART-" + uid, "Brake Pads " + uid, 60.00, 20, 5));

        Invoice invoice = new Invoice(UUID.randomUUID().toString(), customer, car, 8.0, BillingPolicy.defaultPolicy());
        ServiceTask task = new ServiceTask("Replace brake pads", mechanic);
        task.setInvoice(invoice);
        invoice.addTask(task);

        // Add part at current price (60.00)
        task.addUsedPart(item, 2);
        assertThat(task.getUsedParts()).hasSize(1);
        assertThat(task.getUsedParts().get(0).getUnitPrice()).isEqualTo(60.00);

        invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();

        // Now modify the inventory item's unit price in database to 95.00
        InventoryItem fetchedItem = inventoryItemRepository.findById("PART-" + uid).orElseThrow();
        fetchedItem.setUnitPrice(95.00);
        inventoryItemRepository.save(fetchedItem);
        entityManager.flush();
        entityManager.clear();

        // Reload the invoice and task; snapshot price on TaskUsedPart must remain 60.00
        Invoice reloadedInvoice = invoiceRepository.findByIdWithTasksAndDetails(invoice.getInvoiceId()).orElseThrow();
        ServiceTask reloadedTask = reloadedInvoice.getTaskList().get(0);
        assertThat(reloadedTask.getUsedParts()).hasSize(1);

        TaskUsedPart snapshotPart = reloadedTask.getUsedParts().get(0);
        assertThat(snapshotPart.getUnitPrice())
                .as("Stored snapshot unit price must remain unchanged when inventory price changes")
                .isEqualTo(60.00);
        assertThat(snapshotPart.getSubtotal())
                .as("Subtotal snapshot must remain 2 * 60.00 = 120.00")
                .isEqualTo(120.00);
    }

    @Test
    @EnabledIf("isDbAvailable")
    @Transactional
    @DisplayName("Check 5: CHECK constraint on inventory_items rejects negative stock_quantity in PostgreSQL")
    void check5_checkConstraintRejectsNegativeStock() {
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String partCode = "CHK-" + uid;

        // Direct SQL insert with stock_quantity < 0 must be rejected by PostgreSQL CHECK (stock_quantity >= 0)
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    "INSERT INTO inventory_items (part_code, part_name, unit_price, stock_quantity, reorder_threshold) VALUES (?, 'Check Part', 15.00, -5, 2)",
                    partCode
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
