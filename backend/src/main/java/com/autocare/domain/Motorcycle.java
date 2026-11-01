package com.autocare.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Demonstrates Inheritance and Dynamic Polymorphism:
 * Concrete vehicle subclass representing motorcycles.
 * Overrides getBaseServiceFee() to return the fixed constant 30.00 without type checks or branching.
 */
@Entity
@DiscriminatorValue("MOTORCYCLE")
public class Motorcycle extends Vehicle {

    private static final double BASE_FEE = 30.00;

    @Column(name = "engine_cc")
    private int engineCC;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Motorcycle() {
        super();
    }

    /**
     * Parameterized constructor for Motorcycle.
     *
     * @param licensePlate registration license plate
     * @param brand        motorcycle manufacturer
     * @param model        motorcycle model
     * @param owner        registered owner
     * @param engineCC     engine displacement in cubic centimetres (must be > 0)
     */
    public Motorcycle(String licensePlate, String brand, String model, Customer owner, int engineCC) {
        super(licensePlate, brand, model, owner);
        if (engineCC <= 0) {
            throw new IllegalArgumentException("Engine displacement (CC) must be strictly positive");
        }
        this.engineCC = engineCC;
    }

    /**
     * Convenience constructor without an owner (to be set upon customer registration).
     */
    public Motorcycle(String licensePlate, String brand, String model, int engineCC) {
        this(licensePlate, brand, model, null, engineCC);
    }

    public int getEngineCC() {
        return engineCC;
    }

    public void setEngineCC(int engineCC) {
        if (engineCC <= 0) {
            throw new IllegalArgumentException("Engine displacement (CC) must be strictly positive");
        }
        this.engineCC = engineCC;
    }

    /**
     * Demonstrates dynamic polymorphism: overrides Vehicle.getBaseServiceFee()
     * with the fixed Motorcycle service fee.
     *
     * @return 30.00
     */
    @Override
    public double getBaseServiceFee() {
        return BASE_FEE;
    }

    /**
     * Demonstrates dynamic polymorphism: returns "Motorcycle".
     */
    @Override
    public String getVehicleType() {
        return "Motorcycle";
    }
}
