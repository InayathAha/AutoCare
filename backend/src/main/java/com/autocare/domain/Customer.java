package com.autocare.domain;

import com.autocare.exception.DuplicateResourceException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demonstrates Inheritance and Encapsulation:
 * Concrete entity representing a garage customer with registered vehicles and loyalty points.
 * Invariants on vehicle registration and loyalty point redemption are enforced directly within the entity.
 */
@Entity
@Table(name = "customers")
@AttributeOverride(name = "id", column = @Column(name = "customer_id", length = 36, nullable = false))
public class Customer extends Person {

    @OneToMany(mappedBy = "owner")
    private List<Vehicle> registeredVehicles = new ArrayList<>();

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Customer() {
        super();
    }

    /**
     * Parameterized constructor for Customer.
     * Initial loyalty points start at 0.
     *
     * @param id    unique UUID identifier
     * @param name  full customer name
     * @param phone contact telephone number
     * @param email unique email address
     */
    public Customer(String id, String name, String phone, String email) {
        super(id, name, phone, email);
        this.loyaltyPoints = 0;
        this.createdAt = Instant.now();
    }

    /**
     * Registers a vehicle to this customer.
     * Enforces the invariant that duplicate license plates are rejected and bidirectional association is linked.
     *
     * @param v vehicle to register
     * @throws IllegalArgumentException   if vehicle is null
     * @throws DuplicateResourceException if vehicle plate is already registered for this customer
     */
    public void registerVehicle(Vehicle v) {
        if (v == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        boolean exists = registeredVehicles.stream()
                .anyMatch(existing -> existing.getLicensePlate().equalsIgnoreCase(v.getLicensePlate()));
        if (exists) {
            throw new DuplicateResourceException(
                    "Vehicle with license plate " + v.getLicensePlate() + " is already registered to " + getName()
            );
        }
        v.setOwner(this);
        registeredVehicles.add(v);
    }

    /**
     * Returns an unmodifiable view of registered vehicles to preserve encapsulation.
     *
     * @return unmodifiable list of vehicles
     */
    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(registeredVehicles);
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    /**
     * Adds earned loyalty points.
     *
     * @param points number of positive points to add
     * @throws IllegalArgumentException if points <= 0
     */
    public void addLoyaltyPoints(int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Loyalty points to add must be positive");
        }
        this.loyaltyPoints += points;
    }

    /**
     * Deducts loyalty points if valid and sufficient balance exists.
     *
     * @param points number of points to redeem
     * @return true if redemption succeeded; false if points <= 0 or exceeds available balance
     */
    public boolean redeemPoints(int points) {
        if (points <= 0 || points > this.loyaltyPoints) {
            return false;
        }
        this.loyaltyPoints -= points;
        return true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Demonstrates dynamic polymorphism: overrides Person.displayProfile() to output customer details.
     */
    @Override
    public void displayProfile() {
        System.out.println(describe());
    }

    @Override
    public String describe() {
        return String.format(
                "Customer[id=%s, name=%s, phone=%s, email=%s, loyaltyPoints=%d, vehiclesCount=%d]",
                getId(), getName(), getPhone(), getEmail(), loyaltyPoints, registeredVehicles.size()
        );
    }
}
