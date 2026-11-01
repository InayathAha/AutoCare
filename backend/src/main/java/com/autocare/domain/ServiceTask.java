package com.autocare.domain;

import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.InsufficientStockException;
import com.autocare.exception.InvalidStateTransitionException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Demonstrates Domain Invariant Management and State Pattern Guarding:
 * Represents a discrete repair or service task performed by a mechanic.
 * <p>
 * Key invariants:
 * 1. Status strictly moves forward: PENDING -> IN_PROGRESS -> COMPLETED (all other moves rejected).
 * 2. Immutability: Once COMPLETED, no parts, labor hours, or description may be mutated.
 * 3. Consumed parts deduct stock immediately and capture historical price snapshots.
 * 4. Costs (partsCost, laborCharge, totalTaskCost) are computed solely through business methods.
 */
@Entity
@Table(name = "service_tasks")
public class ServiceTask {

    @Id
    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_mechanic_id", nullable = false)
    private Mechanic assignedMechanic;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskUsedPart> usedParts = new ArrayList<>();

    @Column(name = "billable_hours", precision = 5, scale = 2, nullable = false)
    private BigDecimal billableHours = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "labor_charge", precision = 10, scale = 2, nullable = false)
    private BigDecimal laborCharge = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "parts_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal partsCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "total_task_cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalTaskCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Transient
    private Instant completedAt;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected ServiceTask() {
    }

    /**
     * Parameterized constructor generating a random UUID.
     *
     * @param description      diagnostic and repair details
     * @param assignedMechanic mechanic assigned to carry out the task
     */
    public ServiceTask(String description, Mechanic assignedMechanic) {
        this(UUID.randomUUID().toString(), description, assignedMechanic);
    }

    /**
     * Full parameterized constructor for ServiceTask.
     *
     * @param taskId           unique identifier
     * @param description      diagnostic and repair details
     * @param assignedMechanic mechanic assigned to carry out the task
     */
    public ServiceTask(String taskId, String description, Mechanic assignedMechanic) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        if (assignedMechanic == null) {
            throw new IllegalArgumentException("Assigned mechanic cannot be null");
        }
        this.taskId = taskId.trim();
        this.description = description.trim();
        this.assignedMechanic = assignedMechanic;
        this.status = TaskStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        assertNotCompleted();
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        this.description = description.trim();
    }

    public String getStatus() {
        return status.name();
    }

    public TaskStatus getTaskStatus() {
        return status;
    }

    public Mechanic getAssignedMechanic() {
        return assignedMechanic;
    }

    public void setAssignedMechanic(Mechanic assignedMechanic) {
        assertNotCompleted();
        if (assignedMechanic == null) {
            throw new IllegalArgumentException("Assigned mechanic cannot be null");
        }
        this.assignedMechanic = assignedMechanic;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public double getBillableHours() {
        return billableHours.doubleValue();
    }

    public double getLaborCharge() {
        return laborCharge.doubleValue();
    }

    public double getPartsCost() {
        return partsCost.doubleValue();
    }

    public double getTotalTaskCost() {
        return totalTaskCost.doubleValue();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<TaskUsedPart> getUsedParts() {
        return Collections.unmodifiableList(usedParts);
    }

    /**
     * Builds and returns an unmodifiable view of used items mapped to consumed quantities per UML.
     *
     * @return unmodifiable Map of InventoryItem to consumed quantity
     */
    public Map<InventoryItem, Integer> getUsedItems() {
        Map<InventoryItem, Integer> map = new LinkedHashMap<>();
        for (TaskUsedPart part : usedParts) {
            map.put(part.getItem(), part.getQuantity());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Guards lifecycle state transitions.
     * Strictly allows only two forward transitions:
     * 1. PENDING -> IN_PROGRESS
     * 2. IN_PROGRESS -> COMPLETED
     * All other transitions throw InvalidStateTransitionException.
     *
     * @param targetStatus string representation of destination status
     * @throws InvalidStateTransitionException on any invalid transition attempt
     */
    public void setStatus(String targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }
        TaskStatus next;
        try {
            next = TaskStatus.valueOf(targetStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStateTransitionException("Unknown task status: " + targetStatus);
        }
        transitionTo(next);
    }

    /**
     * Starts the service task, transitioning from PENDING to IN_PROGRESS.
     */
    public void startTask() {
        transitionTo(TaskStatus.IN_PROGRESS);
    }

    /**
     * Marks the service task completed, transitioning from IN_PROGRESS to COMPLETED.
     */
    public void markCompleted() {
        transitionTo(TaskStatus.COMPLETED);
    }

    private void transitionTo(TaskStatus next) {
        boolean valid = (this.status == TaskStatus.PENDING && next == TaskStatus.IN_PROGRESS)
                || (this.status == TaskStatus.IN_PROGRESS && next == TaskStatus.COMPLETED);

        if (!valid) {
            throw new InvalidStateTransitionException(
                    "Invalid state transition: cannot transition task " + taskId + " from " + this.status + " to " + next
            );
        }

        this.status = next;
        if (next == TaskStatus.COMPLETED) {
            this.completedAt = Instant.now();
        }
    }

    /**
     * Adds a consumable part to this task.
     * Invariants:
     * - Task must not be in COMPLETED state.
     * - Quantity must be > 0.
     * - Deducts stock from the inventory item; throws InsufficientStockException on shortage without mutating stock.
     * - Merges quantity if part was already added, preserving the initial snapshot price.
     * - Recomputes partsCost and totalTaskCost.
     *
     * @param item inventory part to consume
     * @param qty  positive quantity
     * @throws BusinessRuleViolationException if task is already completed
     * @throws IllegalArgumentException        if item is null or qty <= 0
     * @throws InsufficientStockException      if stock cannot satisfy the requested quantity
     */
    public void addUsedPart(InventoryItem item, int qty) {
        assertNotCompleted();
        if (item == null) {
            throw new IllegalArgumentException("Inventory item cannot be null");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be strictly positive");
        }

        boolean deducted = item.deductStock(qty);
        if (!deducted) {
            throw new InsufficientStockException(
                    "Insufficient stock for part " + item.getPartCode() +
                            ": requested " + qty + ", available " + item.getStockQuantity()
            );
        }

        Optional<TaskUsedPart> existingPart = usedParts.stream()
                .filter(p -> p.getItem().getPartCode().equalsIgnoreCase(item.getPartCode()))
                .findFirst();

        if (existingPart.isPresent()) {
            existingPart.get().addQuantity(qty);
        } else {
            TaskUsedPart newPart = new TaskUsedPart(this, item, qty, item.getUnitPrice());
            usedParts.add(newPart);
        }

        recalculatePartsCost();
        calculateTaskCost();
    }

    /**
     * Records billable labor hours and computes labor charges.
     * Invariants:
     * - Task must not be in COMPLETED state.
     * - Hours and ratePerHour must be >= 0.
     *
     * @param hours       billable labor hours
     * @param ratePerHour hourly labor charge rate
     * @throws BusinessRuleViolationException if task is already completed
     * @throws IllegalArgumentException        if hours or rate is negative
     */
    public void recordBillableHours(double hours, double ratePerHour) {
        assertNotCompleted();
        if (hours < 0) {
            throw new IllegalArgumentException("Billable hours cannot be negative");
        }
        if (ratePerHour < 0) {
            throw new IllegalArgumentException("Hourly rate cannot be negative");
        }
        this.billableHours = BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
        this.laborCharge = this.billableHours.multiply(BigDecimal.valueOf(ratePerHour)).setScale(2, RoundingMode.HALF_UP);
        calculateTaskCost();
    }

    /**
     * Records billable labor hours using the configurable hourly rate in the BillingPolicy.
     *
     * @param hours  billable labor hours
     * @param policy immutable billing policy providing laborRatePerHour
     */
    public void recordBillableHours(double hours, BillingPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Billing policy cannot be null");
        }
        recordBillableHours(hours, policy.laborRatePerHour());
    }

    /**
     * Calculates total task cost as partsCost + laborCharge and synchronizes totalTaskCost column.
     *
     * @return total task cost
     */
    public double calculateTaskCost() {
        this.totalTaskCost = this.partsCost.add(this.laborCharge).setScale(2, RoundingMode.HALF_UP);
        return this.totalTaskCost.doubleValue();
    }

    private void recalculatePartsCost() {
        BigDecimal sum = BigDecimal.ZERO;
        for (TaskUsedPart part : usedParts) {
            sum = sum.add(part.getSubtotalBigDecimal());
        }
        this.partsCost = sum.setScale(2, RoundingMode.HALF_UP);
    }

    private void assertNotCompleted() {
        if (this.status == TaskStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Cannot mutate a completed service task: " + taskId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceTask that)) return false;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
}
