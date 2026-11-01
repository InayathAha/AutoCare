package com.autocare.service;

import com.autocare.domain.*;
import com.autocare.dto.DiagnosticsRequest;
import com.autocare.dto.LaborRequest;
import com.autocare.dto.TaskPartRequest;
import com.autocare.dto.TaskRequest;
import com.autocare.dto.TaskResponse;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.InsufficientStockException;
import com.autocare.exception.InvalidStateTransitionException;
import com.autocare.mapper.TaskMapper;
import com.autocare.repository.EmployeeRepository;
import com.autocare.repository.InventoryItemRepository;
import com.autocare.repository.InvoiceRepository;
import com.autocare.repository.ServiceTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private ServiceTaskRepository serviceTaskRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    private final TaskMapper taskMapper = new TaskMapper();
    private BillingPolicy billingPolicy;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        billingPolicy = BillingPolicy.defaultPolicy(); // laborRatePerHour = 20.00
        taskService = new TaskService(
                serviceTaskRepository,
                invoiceRepository,
                employeeRepository,
                inventoryItemRepository,
                taskMapper,
                billingPolicy
        );
    }

    @Test
    @DisplayName("create() attaches task to OPEN invoice and assigned mechanic")
    void create_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car);

        when(invoiceRepository.findById("inv1")).thenReturn(Optional.of(invoice));

        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        when(employeeRepository.findById("m1")).thenReturn(Optional.of(mechanic));

        when(serviceTaskRepository.save(any(ServiceTask.class))).thenAnswer(i -> i.getArgument(0));

        TaskRequest req = new TaskRequest("Oil Change", "m1");
        TaskResponse actual = taskService.create("inv1", req);

        assertThat(actual.description()).isEqualTo("Oil Change");
        assertThat(actual.status()).isEqualTo("PENDING");
        assertThat(actual.assignedMechanicId()).isEqualTo("m1");
        assertThat(actual.assignedMechanicName()).isEqualTo("Bob");
        assertThat(invoice.getTaskList()).hasSize(1);
        verify(serviceTaskRepository).save(any(ServiceTask.class));
    }

    @Test
    @DisplayName("create() throws BusinessRuleViolationException when assigned employee is not a mechanic")
    void create_nonMechanic_throws() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car);
        when(invoiceRepository.findById("inv1")).thenReturn(Optional.of(invoice));

        Manager manager = new Manager("mgr1", "Carol", "0773333333", "carol@test.com", 2000.0, 300.0);
        when(employeeRepository.findById("mgr1")).thenReturn(Optional.of(manager));

        TaskRequest req = new TaskRequest("Oil Change", "mgr1");
        assertThatThrownBy(() -> taskService.create("inv1", req))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Mechanic");
    }

    @Test
    @DisplayName("start() transitions task from PENDING to IN_PROGRESS")
    void start_success() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Brake pad replacement", mechanic);
        when(serviceTaskRepository.findById("t1")).thenReturn(Optional.of(task));

        TaskResponse actual = taskService.start("t1");

        assertThat(actual.status()).isEqualTo("IN_PROGRESS");
        assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("complete() calls mechanic.completeServiceTask and marks COMPLETED")
    void complete_success() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Brake pad replacement", mechanic);
        task.startTask(); // must be IN_PROGRESS first
        when(serviceTaskRepository.findById("t1")).thenReturn(Optional.of(task));

        TaskResponse actual = taskService.complete("t1");

        assertThat(actual.status()).isEqualTo("COMPLETED");
        assertThat(task.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("complete() throws InvalidStateTransitionException if completing directly from PENDING")
    void complete_fromPending_throws() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Brake pad replacement", mechanic);
        when(serviceTaskRepository.findById("t1")).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.complete("t1"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("updateDiagnostics() updates task description while not completed")
    void updateDiagnostics_success() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Initial description", mechanic);
        when(serviceTaskRepository.findById("t1")).thenReturn(Optional.of(task));

        DiagnosticsRequest req = new DiagnosticsRequest("Updated diagnostic findings");
        taskService.updateDiagnostics("t1", req);

        assertThat(task.getDescription()).isEqualTo("Updated diagnostic findings");
    }

    @Test
    @DisplayName("addPart() deducts stock and attaches used part snapshot")
    void addPart_success() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Oil Service", mechanic);
        when(serviceTaskRepository.findById("t1")).thenReturn(Optional.of(task));

        InventoryItem item = new InventoryItem("OIL-5W30", "Engine Oil", 8.00, 20, 5);
        when(inventoryItemRepository.findById("OIL-5W30")).thenReturn(Optional.of(item));
        when(inventoryItemRepository.findByPartCodeForUpdate("OIL-5W30")).thenReturn(Optional.of(item));

        TaskPartRequest req = new TaskPartRequest("OIL-5W30", 3);
        TaskResponse response = taskService.addPart("t1", req);

        assertThat(item.getStockQuantity()).isEqualTo(17);
        assertThat(task.getPartsCost()).isEqualTo(24.00); // 3 * 8.00
        assertThat(task.getUsedParts()).hasSize(1);
        assertThat(response.partsCost()).isEqualTo(24.00);
        assertThat(response.usedParts()).hasSize(1);
    }

    @Test
    @DisplayName("addPart() throws InsufficientStockException when quantity exceeds stock")
    void addPart_insufficientStock_throws() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Oil Service", mechanic);
        when(serviceTaskRepository.findById("t1")).thenReturn(Optional.of(task));

        InventoryItem item = new InventoryItem("OIL-5W30", "Engine Oil", 8.00, 2, 5);
        when(inventoryItemRepository.findById("OIL-5W30")).thenReturn(Optional.of(item));
        when(inventoryItemRepository.findByPartCodeForUpdate("OIL-5W30")).thenReturn(Optional.of(item));

        TaskPartRequest req = new TaskPartRequest("OIL-5W30", 5);
        assertThatThrownBy(() -> taskService.addPart("t1", req))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(item.getStockQuantity()).isEqualTo(2); // unchanged
    }

    @Test
    @DisplayName("recordLabor() calculates labor charges using configured billing rate")
    void recordLabor_success() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Engine Tuneup", mechanic);
        when(serviceTaskRepository.findById("t1")).thenReturn(Optional.of(task));

        LaborRequest req = new LaborRequest(2.5);
        TaskResponse response = taskService.recordLabor("t1", req);

        assertThat(task.getBillableHours()).isEqualTo(2.5);
        assertThat(task.getLaborCharge()).isEqualTo(50.00); // 2.5 * 20.00/h
        assertThat(task.calculateTaskCost()).isEqualTo(50.00);
        assertThat(response.billableHours()).isEqualTo(2.5);
        assertThat(response.laborCharge()).isEqualTo(50.00);
        assertThat(response.totalTaskCost()).isEqualTo(50.00);
    }
}
