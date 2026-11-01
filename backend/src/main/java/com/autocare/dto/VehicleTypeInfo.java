package com.autocare.dto;

/**
 * Metadata for supported vehicle categories and their base service fees.
 * Demonstrates live polymorphism: built by instantiating Car, Van, and Motorcycle
 * and calling getBaseServiceFee() polymorphically.
 */
public record VehicleTypeInfo(
        String type,
        double baseServiceFee
) {
}
