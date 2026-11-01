package com.autocare;

import com.autocare.domain.*;
import com.autocare.dto.*;
import com.autocare.exception.InsufficientStockException;
import com.autocare.repository.*;
import com.autocare.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 5 / T5.1 End-to-End Scenarios:
 * 1. Register customer + Car -> open invoice -> create task -> add part -> labour -> start -> complete -> issue -> settle:
 *    totals match hand calculation, customer gains loyalty points, stock reduced.
 * 2. Insufficient stock returns 409 and leaves stock and task unchanged.
 * 3. Two concurrent addPart calls on a part with stock for only one: exactly one succeeds (pessimistic locking).
 * 4. Percentage discount above cap without manager returns 422; with valid manager succeeds.
 * 5. Delete customer with vehicles returns 409 FK_CONSTRAINT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("T5.1 - End-to-End System Scenarios")
class EndToEndScenariosTest {

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
        }
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
                        if (trimmed.startsWith("#") || !trimmed.contains("=")) continue;
                        int idx = trimmed.indexOf('=');
                        map.put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim());
                    }
                } catch (IOException ignored) {}
                break;
            }
        }
        return map;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ServiceTaskRepository serviceTaskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskService taskService;

    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("Scenario 1: Full lifecycle - Customer, Car, Mechanic, Part, Task, Issue, Settle, Points, Stock")
    void scenario1_fullLifecycleWorkflow() throws Exception {
        // 1. Register Customer
        String custEmail = "cust_" + uniqueSuffix + "@e2e.test";
        CustomerRequest custReq = new CustomerRequest("John Doe " + uniqueSuffix, "0771234567", custEmail);
        MvcResult custResult = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();
        CustomerResponse custRes = objectMapper.readValue(custResult.getResponse().getContentAsString(), CustomerResponse.class);
        String customerId = custRes.id();

        // 2. Register Car (Polymorphic base fee = 50.00)
        String plate = "CAR-" + uniqueSuffix.toUpperCase();
        VehicleRequest vehReq = new VehicleRequest("CAR", plate, "Toyota", "Corolla", 5, null, null);
        mockMvc.perform(post("/api/v1/customers/" + customerId + "/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate", is(plate)))
                .andExpect(jsonPath("$.baseServiceFee", is(50.0)));

        // 3. Register Mechanic (Base 1000, hourly 20.0)
        String mechEmail = "mech_" + uniqueSuffix + "@e2e.test";
        EmployeeRequest mechReq = new EmployeeRequest("MECHANIC", "Bob Mechanic", "0772223333", mechEmail, 1000.0, "Engine", 20.0, null);
        MvcResult mechResult = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mechReq)))
                .andExpect(status().isCreated())
                .andReturn();
        EmployeeResponse mechRes = objectMapper.readValue(mechResult.getResponse().getContentAsString(), EmployeeResponse.class);
        String mechanicId = mechRes.id();

        // 4. Create Inventory Item (Stock 10, unit price 15.00)
        String partCode = "OIL-" + uniqueSuffix.toUpperCase();
        InventoryItemRequest partReq = new InventoryItemRequest(partCode, "Engine Oil 5W30", 15.0, 10, 2);
        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partCode", is(partCode)))
                .andExpect(jsonPath("$.stockQuantity", is(10)));

        // 5. Open Invoice
        OpenInvoiceRequest invReq = new OpenInvoiceRequest(customerId, plate);
        MvcResult invResult = mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").isNotEmpty())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.baseServiceFee", is(50.0)))
                .andReturn();
        InvoiceResponse invRes = objectMapper.readValue(invResult.getResponse().getContentAsString(), InvoiceResponse.class);
        String invoiceId = invRes.invoiceId();

        // 6. Create ServiceTask on Invoice
        TaskRequest taskReq = new TaskRequest("Engine Service & Filter Change", mechanicId);
        MvcResult taskResult = mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();
        TaskResponse taskRes = objectMapper.readValue(taskResult.getResponse().getContentAsString(), TaskResponse.class);
        String taskId = taskRes.taskId();

        // 7. Add Part to Task (2 units @ 15.00 = 30.00)
        TaskPartRequest taskPartReq = new TaskPartRequest(partCode, 2);
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskPartReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partsCost", is(30.0)));

        // 8. Record Labour (1.5 hours @ 20.00/h = 30.00)
        LaborRequest laborReq = new LaborRequest(1.5);
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/labor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laborReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billableHours", is(1.5)))
                .andExpect(jsonPath("$.laborCharge", is(30.0)))
                .andExpect(jsonPath("$.totalTaskCost", is(60.0))); // 30.0 parts + 30.0 labor

        // 9. Start Task (PENDING -> IN_PROGRESS)
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // 10. Complete Task (IN_PROGRESS -> COMPLETED)
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // 11. Issue Invoice
        // Calculation:
        // Base Fee: 50.00
        // Task Cost: 60.00
        // Subtotal: 110.00
        // Tax (8% default): 8.80
        // Total: 118.80
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ISSUED")))
                .andExpect(jsonPath("$.subtotal", is(110.0)))
                .andExpect(jsonPath("$.taxRate", is(8.0)))
                .andExpect(jsonPath("$.taxAmount", is(8.8)))
                .andExpect(jsonPath("$.totalAmount", is(118.8)));

        // 12. Settle Invoice (ISSUED -> PAID)
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/settle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.settledAt").isNotEmpty());

        // 13. Verify Customer Gained Loyalty Points: 1 point per $10 of total (118.8 * 0.1 = 11 points)
        Customer customerInDb = customerRepository.findById(customerId).orElseThrow();
        assertThat(customerInDb.getLoyaltyPoints()).isEqualTo(11);

        // 14. Verify Stock Reduced: 10 - 2 = 8
        InventoryItem itemInDb = inventoryItemRepository.findById(partCode).orElseThrow();
        assertThat(itemInDb.getStockQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Scenario 2: Insufficient stock returns 409 and leaves stock and task unchanged")
    void scenario2_insufficientStock() throws Exception {
        // Setup customer, car, mechanic, invoice, and task
        String custEmail = "cust2_" + uniqueSuffix + "@e2e.test";
        Customer customer = customerRepository.save(new Customer(UUID.randomUUID().toString(), "Cust 2", "0771112222", custEmail));
        Vehicle car = vehicleRepository.save(new Car("CAR2-" + uniqueSuffix.toUpperCase(), "Honda", "Civic", customer, 5));
        Mechanic mechanic = employeeRepository.save(new Mechanic(UUID.randomUUID().toString(), "Mech 2", "0772224444", "mech2_" + uniqueSuffix + "@e2e.test", 1000.0, "General", 20.0));
        Invoice invoice = new Invoice(UUID.randomUUID().toString(), customer, car);
        ServiceTask task = new ServiceTask(UUID.randomUUID().toString(), "Brake pad replacement", mechanic);
        invoice.addTask(task);
        invoiceRepository.save(invoice);

        // Part with stock = 2
        String partCode = "BRK-" + uniqueSuffix.toUpperCase();
        inventoryItemRepository.save(new InventoryItem(partCode, "Brake Pads", 45.0, 2, 1));

        // Request 5 units (exceeds stock of 2)
        TaskPartRequest req = new TaskPartRequest(partCode, 5);

        mockMvc.perform(post("/api/v1/tasks/" + task.getTaskId() + "/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("insufficient stock")));

        // Verify stock is untouched in database
        InventoryItem itemInDb = inventoryItemRepository.findById(partCode).orElseThrow();
        assertThat(itemInDb.getStockQuantity()).isEqualTo(2);

        // Verify task has 0 used parts in database
        TaskResponse taskInDb = taskService.get(task.getTaskId());
        assertThat(taskInDb.usedParts()).isEmpty();
    }

    @Test
    @DisplayName("Scenario 3: Two concurrent addPart calls on a part with stock for only one: exactly one succeeds")
    void scenario3_concurrentAddPart() throws Exception {
        // Setup customer, car, mechanic, invoice, and task
        String custEmail = "cust3_" + uniqueSuffix + "@e2e.test";
        Customer customer = customerRepository.save(new Customer(UUID.randomUUID().toString(), "Cust 3", "0773334444", custEmail));
        Vehicle car = vehicleRepository.save(new Car("CAR3-" + uniqueSuffix.toUpperCase(), "Mazda", "3", customer, 5));
        Mechanic mechanic = employeeRepository.save(new Mechanic(UUID.randomUUID().toString(), "Mech 3", "0773335555", "mech3_" + uniqueSuffix + "@e2e.test", 1000.0, "General", 20.0));
        Invoice invoice = new Invoice(UUID.randomUUID().toString(), customer, car);
        ServiceTask task = new ServiceTask(UUID.randomUUID().toString(), "Air Filter Replacement", mechanic);
        invoice.addTask(task);
        invoiceRepository.save(invoice);

        // Part with stock = 1
        String partCode = "FLT-" + uniqueSuffix.toUpperCase();
        inventoryItemRepository.save(new InventoryItem(partCode, "Air Filter", 25.0, 1, 0));

        // Attempt concurrent addPart from two parallel threads
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // ensure simultaneous execution
                    taskService.addPart(task.getTaskId(), new TaskPartRequest(partCode, 1));
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // release threads simultaneously

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly one call must succeed and one must fail
        assertThat(successCount.get())
                .as("Exactly one concurrent thread should deduct the stock")
                .isEqualTo(1);
        assertThat(failureCount.get())
                .as("The losing thread must fail due to stock depletion")
                .isEqualTo(1);

        // Final stock quantity must be 0 (never negative)
        InventoryItem itemInDb = inventoryItemRepository.findById(partCode).orElseThrow();
        assertThat(itemInDb.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Scenario 4: Percentage discount above cap without manager returns 422; with valid manager succeeds")
    void scenario4_managerAuthorizationForDiscount() throws Exception {
        // Setup customer, car, invoice
        String custEmail = "cust4_" + uniqueSuffix + "@e2e.test";
        Customer customer = customerRepository.save(new Customer(UUID.randomUUID().toString(), "Cust 4", "0774445555", custEmail));
        Vehicle car = vehicleRepository.save(new Car("CAR4-" + uniqueSuffix.toUpperCase(), "Nissan", "Leaf", customer, 5));
        Invoice invoice = invoiceRepository.save(new Invoice(UUID.randomUUID().toString(), customer, car));

        // Attempt 20% discount without manager (policy limit is 15%)
        DiscountPercentageRequest unapprovedReq = new DiscountPercentageRequest(20.0, null);

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getInvoiceId() + "/discount/percentage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unapprovedReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_DISCOUNT")))
                .andExpect(jsonPath("$.message", containsString("manager approval required")));

        // Register a Manager
        String mgrEmail = "mgr_" + uniqueSuffix + "@e2e.test";
        EmployeeRequest mgrReq = new EmployeeRequest("MANAGER", "Director Boss", "0779998888", mgrEmail, 2000.0, null, null, 400.0);
        MvcResult mgrResult = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mgrReq)))
                .andExpect(status().isCreated())
                .andReturn();
        EmployeeResponse mgrRes = objectMapper.readValue(mgrResult.getResponse().getContentAsString(), EmployeeResponse.class);

        // Apply 20% discount WITH valid manager approval
        DiscountPercentageRequest approvedReq = new DiscountPercentageRequest(20.0, mgrRes.id());

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getInvoiceId() + "/discount/percentage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount", is(10.0))); // 20% of 50.00 base fee = 10.00
    }

    @Test
    @DisplayName("Scenario 5: Delete customer with vehicles returns 409 FK_CONSTRAINT")
    void scenario5_deleteCustomerWithVehicles_returns409() throws Exception {
        // Setup customer with registered vehicle
        String custEmail = "cust5_" + uniqueSuffix + "@e2e.test";
        Customer customer = customerRepository.save(new Customer(UUID.randomUUID().toString(), "Cust 5", "0775556666", custEmail));
        Vehicle car = vehicleRepository.save(new Car("CAR5-" + uniqueSuffix.toUpperCase(), "BMW", "320i", customer, 5));
        customer.registerVehicle(car);
        customerRepository.save(customer);

        // Attempt to DELETE customer while vehicle is registered
        mockMvc.perform(delete("/api/v1/customers/" + customer.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("FK_CONSTRAINT")));

        // Verify customer and vehicle still exist in database
        assertThat(customerRepository.findById(customer.getId())).isPresent();
        assertThat(vehicleRepository.findById(car.getLicensePlate())).isPresent();
    }
}
