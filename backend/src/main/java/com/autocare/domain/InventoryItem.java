package com.autocare.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Demonstrates Encapsulation and Domain Invariant Enforcement:
 * Represents garage inventory parts and consumables.
 * <p>
 * The stock quantity invariant is strictly enforced: stock can NEVER drop below zero.
 * Stock mutations are only permitted through guarded business methods: deductStock() and restock().
 * Money is stored internally as BigDecimal (scale 2, HALF_UP) while exposing double accessors per UML.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(name = "part_code", length = 50, nullable = false)
    private String partCode;

    @Column(name = "part_name", length = 100, nullable = false)
    private String partName;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    @Column(name = "last_restocked_at")
    private Instant lastRestockedAt;

    @Transient
    private Long version = 0L;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected InventoryItem() {
    }

    /**
     * Parameterized constructor initializing inventory item with validated parameters.
     *
     * @param partCode         unique part code identifier
     * @param partName         descriptive component name
     * @param unitPrice        price per unit (must be >= 0)
     * @param stockQuantity    initial stock quantity (must be >= 0)
     * @param reorderThreshold low stock alert threshold (must be >= 0)
     */
    public InventoryItem(String partCode, String partName, double unitPrice, int stockQuantity, int reorderThreshold) {
        if (partCode == null || partCode.isBlank()) {
            throw new IllegalArgumentException("Part code cannot be null or blank");
        }
        if (partName == null || partName.isBlank()) {
            throw new IllegalArgumentException("Part name cannot be null or blank");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Initial stock quantity cannot be negative");
        }
        if (reorderThreshold < 0) {
            throw new IllegalArgumentException("Reorder threshold cannot be negative");
        }
        this.partCode = partCode.trim().toUpperCase();
        this.partName = partName.trim();
        this.unitPrice = BigDecimal.valueOf(unitPrice).setScale(2, RoundingMode.HALF_UP);
        this.stockQuantity = stockQuantity;
        this.reorderThreshold = reorderThreshold;
        this.lastRestockedAt = Instant.now();
    }

    public String getPartCode() {
        return partCode;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        if (partName == null || partName.isBlank()) {
            throw new IllegalArgumentException("Part name cannot be null or blank");
        }
        this.partName = partName.trim();
    }

    public double getUnitPrice() {
        return unitPrice != null ? unitPrice.doubleValue() : 0.0;
    }

    public void setUnitPrice(double unitPrice) {
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        this.unitPrice = BigDecimal.valueOf(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(int reorderThreshold) {
        if (reorderThreshold < 0) {
            throw new IllegalArgumentException("Reorder threshold cannot be negative");
        }
        this.reorderThreshold = reorderThreshold;
    }

    public Instant getLastRestockedAt() {
        return lastRestockedAt;
    }

    public Long getVersion() {
        return version;
    }

    /**
     * Checks if stock level has dropped to or below the reorder threshold.
     * Boundary rule: returns true when stockQuantity <= reorderThreshold.
     *
     * @return true if item needs replenishment
     */
    public boolean isLowStock() {
        return stockQuantity <= reorderThreshold;
    }

    /**
     * Deducts stock strictly respecting the non-negative invariant.
     *
     * @param quantity number of units to deduct
     * @return true if deduction succeeded; false if quantity <= 0 or quantity > current stock
     */
    public boolean deductStock(int quantity) {
        if (quantity <= 0 || quantity > stockQuantity) {
            return false;
        }
        this.stockQuantity -= quantity;
        return true;
    }

    /**
     * Replenishes inventory stock.
     * Invariant: quantity must be strictly positive.
     * Updates lastRestockedAt timestamp.
     *
     * @param quantity positive number of units added
     * @throws IllegalArgumentException if quantity <= 0
     */
    public void restock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be positive");
        }
        this.stockQuantity += quantity;
        this.lastRestockedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryItem that)) return false;
        return Objects.equals(partCode, that.partCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partCode);
    }
}
