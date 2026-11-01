package com.autocare.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Demonstrates Abstraction and Inheritance:
 * Abstract base class for all vehicle types in the garage (Car, Van, Motorcycle).
 * Subclasses demonstrate dynamic polymorphism by overriding getBaseServiceFee() and getVehicleType().
 * Polymorphic fee calculation occurs without any instanceof checks or type switching.
 */
@Entity
@Table(name = "vehicles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "vehicle_type", length = 20)
public abstract class Vehicle {

    @Id
    @Column(name = "license_plate", length = 20, nullable = false)
    protected String licensePlate;

    @Column(name = "brand", length = 50, nullable = false)
    protected String brand;

    @Column(name = "model", length = 50, nullable = false)
    protected String model;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    protected Customer owner;

    @Column(name = "base_service_fee", precision = 10, scale = 2, nullable = false)
    protected BigDecimal baseServiceFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "registered_at", nullable = false, updatable = false)
    protected Instant registeredAt = Instant.now();

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Vehicle() {
    }

    /**
     * Parameterized constructor for Vehicle.
     *
     * @param licensePlate unique registration plate number
     * @param brand        manufacturer/make
     * @param model        model designation
     * @param owner        customer to whom the vehicle is registered
     */
    public Vehicle(String licensePlate, String brand, String model, Customer owner) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate cannot be null or blank");
        }
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("Brand cannot be null or blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model cannot be null or blank");
        }
        this.licensePlate = licensePlate.trim().toUpperCase();
        this.brand = brand.trim();
        this.model = model.trim();
        this.owner = owner;
        this.registeredAt = Instant.now();
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Customer getOwner() {
        return owner;
    }

    public void setOwner(Customer owner) {
        this.owner = owner;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    /**
     * Demonstrates dynamic polymorphism: each vehicle subclass supplies its own base fee.
     *
     * @return base service fee in GBP/currency
     */
    public abstract double getBaseServiceFee();

    /**
     * Demonstrates dynamic polymorphism: returns human-readable vehicle category name.
     *
     * @return vehicle type string
     */
    public abstract String getVehicleType();

    /**
     * PrePersist lifecycle callback ensuring the denormalized base_service_fee column
     * mirrors the polymorphic getBaseServiceFee() value for database reporting.
     */
    @PrePersist
    public void prePersist() {
        this.baseServiceFee = BigDecimal.valueOf(getBaseServiceFee()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle other)) return false;
        return Objects.equals(licensePlate, other.licensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licensePlate);
    }
}
