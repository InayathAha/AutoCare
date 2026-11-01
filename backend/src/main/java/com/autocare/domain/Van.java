package com.autocare.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Demonstrates Inheritance and Dynamic Polymorphism:
 * Concrete vehicle subclass representing commercial vans.
 * Overrides getBaseServiceFee() to return the fixed constant 85.00 without type checks or branching.
 */
@Entity
@DiscriminatorValue("VAN")
public class Van extends Vehicle {

    private static final double BASE_FEE = 85.00;

    @Column(name = "cargo_volume", precision = 6, scale = 2)
    private BigDecimal cargoVolume;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Van() {
        super();
    }

    /**
     * Parameterized constructor for Van.
     *
     * @param licensePlate registration license plate
     * @param brand        van manufacturer
     * @param model        van model
     * @param owner        registered owner
     * @param cargoVolume  cargo cargo capacity in cubic meters (must be > 0)
     */
    public Van(String licensePlate, String brand, String model, Customer owner, double cargoVolume) {
        super(licensePlate, brand, model, owner);
        if (cargoVolume <= 0) {
            throw new IllegalArgumentException("Cargo volume must be strictly positive");
        }
        this.cargoVolume = BigDecimal.valueOf(cargoVolume).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convenience constructor without an owner (to be set upon customer registration).
     */
    public Van(String licensePlate, String brand, String model, double cargoVolume) {
        this(licensePlate, brand, model, null, cargoVolume);
    }

    public double getCargoVolume() {
        return cargoVolume == null ? 0.0 : cargoVolume.doubleValue();
    }

    public void setCargoVolume(double cargoVolume) {
        if (cargoVolume <= 0) {
            throw new IllegalArgumentException("Cargo volume must be strictly positive");
        }
        this.cargoVolume = BigDecimal.valueOf(cargoVolume).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Demonstrates dynamic polymorphism: overrides Vehicle.getBaseServiceFee()
     * with the fixed Van service fee.
     *
     * @return 85.00
     */
    @Override
    public double getBaseServiceFee() {
        return BASE_FEE;
    }

    /**
     * Demonstrates dynamic polymorphism: returns "Van".
     */
    @Override
    public String getVehicleType() {
        return "Van";
    }
}
