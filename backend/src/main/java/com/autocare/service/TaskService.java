package com.autocare.service;

import com.autocare.domain.*;
import com.autocare.dto.DiagnosticsRequest;
import com.autocare.dto.LaborRequest;
import com.autocare.dto.TaskPartRequest;
import com.autocare.dto.TaskRequest;
import com.autocare.dto.TaskResponse;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.InvalidStateTransitionException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.mapper.TaskMapper;
import com.autocare.repository.EmployeeRepository;
import com.autocare.repository.InventoryItemRepository;
import com.autocare.repository.InvoiceRepository;
import com.autocare.repository.ServiceTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating service tasks, diagnostics, mechanic completion, and part consumption.
 * Encapsulates domain invariants: state machine transitions, parts stock deduction, and labor charges.
 */
@Service
@Transactional
public class TaskService {

    private final ServiceTaskRepository serviceTaskRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmployeeRepository employeeRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final TaskMapper taskMapper;
    private final BillingPolicy billingPolicy;

    public TaskService(ServiceTaskRepository serviceTaskRepository,
                       InvoiceRepository invoiceRepository,
                       EmployeeRepository employeeRepository,
                       InventoryItemRepository inventoryItemRepository,
                       TaskMapper taskMapper,
                       BillingPolicy billingPolicy) {
        this.serviceTaskRepository = serviceTaskRepository;
        this.invoiceRepository = invoiceRepository;
        this.employeeRepository = employeeRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.taskMapper = taskMapper;
        this.billingPolicy = billingPolicy;
    }

    public TaskResponse create(String invoiceId, TaskRequest req) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        Employee employee = employeeRepository.findById(req.assignedMechanicId())
                .orElseThrow(() -> new ResourceNotFoundException("Mechanic not found: " + req.assignedMechanicId()));

        if (!(employee instanceof Mechanic mechanic)) {
            throw new BusinessRuleViolationException("Assigned employee is not a Mechanic: " + req.assignedMechanicId());
        }

        ServiceTask task = new ServiceTask(UUID.randomUUID().toString(), req.description(), mechanic);
        invoice.addTask(task); // Enforces invoice is OPEN and recalculates invoice subtotal

        ServiceTask saved = serviceTaskRepository.save(task);
        return taskMapper.toResponse(saved);
    }

    public TaskResponse start(String taskId) {
        ServiceTask task = findTaskOrThrow(taskId);
        task.startTask(); // State pattern guard: PENDING -> IN_PROGRESS
        return taskMapper.toResponse(task);
    }

    public TaskResponse complete(String taskId) {
        ServiceTask task = findTaskOrThrow(taskId);
        Mechanic mechanic = task.getAssignedMechanic();
        if (mechanic == null) {
            throw new BusinessRuleViolationException("Task has no assigned mechanic: " + taskId);
        }
        // Enforces business invariant that only the assigned mechanic can mark the task complete
        mechanic.completeServiceTask(task);
        return taskMapper.toResponse(task);
    }

    public TaskResponse updateDiagnostics(String taskId, DiagnosticsRequest req) {
        ServiceTask task = findTaskOrThrow(taskId);
        task.setDescription(req.description()); // Enforces not COMPLETED
        return taskMapper.toResponse(task);
    }

    public TaskResponse addPart(String taskId, TaskPartRequest req) {
        ServiceTask task = findTaskOrThrow(taskId);
        String code = req.partCode().trim().toUpperCase();
        InventoryItem item = inventoryItemRepository.findByPartCodeForUpdate(code)
                .or(() -> inventoryItemRepository.findById(code))
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + code));

        // Enforces stock availability, decrements inventory stock, and attaches used part snapshot
        task.addUsedPart(item, req.quantity());
        return taskMapper.toResponse(task);
    }

    public TaskResponse recordLabor(String taskId, LaborRequest req) {
        ServiceTask task = findTaskOrThrow(taskId);
        task.recordBillableHours(req.billableHours(), billingPolicy.laborRatePerHour());
        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(String taskId) {
        return taskMapper.toResponse(findTaskOrThrow(taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(String statusStr, String mechanicId) {
        TaskStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = TaskStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidStateTransitionException("Unknown task status filter: " + statusStr);
            }
        }
        String cleanMechanicId = (mechanicId != null && !mechanicId.isBlank()) ? mechanicId.trim() : null;

        List<ServiceTask> tasks = serviceTaskRepository.findByFilter(status, cleanMechanicId);
        return tasks.stream().map(taskMapper::toResponse).toList();
    }

    private ServiceTask findTaskOrThrow(String taskId) {
        return serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Service task not found: " + taskId));
    }
}
