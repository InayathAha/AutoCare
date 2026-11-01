package com.autocare.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Demonstrates Junction Entity and Encapsulation:
 * Represents the association between a ServiceTask and an InventoryItem.
 * Captures snapshot pricing at the exact time the part is consumed so subsequent
 * catalog price changes never alter historical task billing.
 * <p>
 * Package-private constructors and mutators ensure this entity is managed
 * solely through the ServiceTask aggregate root.
 */
@Entity
@Table(name = "task_used_parts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_task_part", columnNames = {"task_id", "part_code"})
})
public class TaskUsedPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "used_part_id")
    private Long usedPartId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ServiceTask task;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "part_code", nullable = false)
    private InventoryItem item;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private Instant attachedAt = Instant.now();

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected TaskUsedPart() {
    }

    /**
     * Package-private constructor used exclusively by ServiceTask.
     * Captures snapshot unit price and calculates line-item subtotal.
     *
     * @param task              owning service task
     * @param item              consumed inventory item
     * @param quantity          number of parts used (must be > 0)
     * @param unitPriceSnapshot price per unit at consumption time (must be >= 0)
     */
    TaskUsedPart(ServiceTask task, InventoryItem item, int quantity, double unitPriceSnapshot) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (item == null) {
            throw new IllegalArgumentException("Inventory item cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be strictly positive");
        }
        if (unitPriceSnapshot < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        this.task = task;
        this.item = item;
        this.quantity = quantity;
        this.unitPrice = BigDecimal.valueOf(unitPriceSnapshot).setScale(2, RoundingMode.HALF_UP);
        this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        this.attachedAt = Instant.now();
    }

    public Long getUsedPartId() {
        return usedPartId;
    }

    public ServiceTask getTask() {
        return task;
    }

    public InventoryItem getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice != null ? unitPrice.doubleValue() : 0.0;
    }

    public double getSubtotal() {
        return subtotal != null ? subtotal.doubleValue() : 0.0;
    }

    BigDecimal getSubtotalBigDecimal() {
        return subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public Instant getAttachedAt() {
        return attachedAt;
    }

    /**
     * Package-private mutator for merging additional quantities of the same part.
     * Recomputes subtotal while retaining original snapshot price.
     *
     * @param additionalQty quantity to add (must be > 0)
     */
    void addQuantity(int additionalQty) {
        if (additionalQty <= 0) {
            throw new IllegalArgumentException("Additional quantity must be strictly positive");
        }
        this.quantity += additionalQty;
        this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskUsedPart that)) return false;
        if (usedPartId != null && that.usedPartId != null) {
            return Objects.equals(usedPartId, that.usedPartId);
        }
        return Objects.equals(task, that.task) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task != null ? task.getTaskId() : null, item != null ? item.getPartCode() : null);
    }
}
