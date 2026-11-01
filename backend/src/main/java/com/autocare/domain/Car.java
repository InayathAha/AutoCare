package com.autocare.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Demonstrates Inheritance and Dynamic Polymorphism:
 * Concrete vehicle subclass representing passenger cars.
 * Overrides getBaseServiceFee() to return the fixed constant 50.00 without type checks or branching.
 */
@Entity
@DiscriminatorValue("CAR")
public class Car extends Vehicle {

    private static final double BASE_FEE = 50.00;

    @Column(name = "seating_capacity")
    private int seatingCapacity;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Car() {
        super();
    }

    /**
     * Parameterized constructor for Car.
     *
     * @param licensePlate    registration license plate
     * @param brand           car manufacturer
     * @param model           car model
     * @param owner           registered owner
     * @param seatingCapacity passenger seating capacity (must be > 0)
     */
    public Car(String licensePlate, String brand, String model, Customer owner, int seatingCapacity) {
        super(licensePlate, brand, model, owner);
        if (seatingCapacity <= 0) {
            throw new IllegalArgumentException("Seating capacity must be strictly positive");
        }
        this.seatingCapacity = seatingCapacity;
    }

    /**
     * Convenience constructor without an owner (to be set upon customer registration).
     */
    public Car(String licensePlate, String brand, String model, int seatingCapacity) {
        this(licensePlate, brand, model, null, seatingCapacity);
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        if (seatingCapacity <= 0) {
            throw new IllegalArgumentException("Seating capacity must be strictly positive");
        }
        this.seatingCapacity = seatingCapacity;
    }

    /**
     * Demonstrates dynamic polymorphism: overrides Vehicle.getBaseServiceFee()
     * with the fixed Car service fee.
     *
     * @return 50.00
     */
    @Override
    public double getBaseServiceFee() {
        return BASE_FEE;
    }

    /**
     * Demonstrates dynamic polymorphism: returns "Car".
     */
    @Override
    public String getVehicleType() {
        return "Car";
    }
}
