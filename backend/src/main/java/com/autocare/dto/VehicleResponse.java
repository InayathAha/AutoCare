package com.autocare.dto;

/**
 * Response payload representing a vehicle.
 */
public record VehicleResponse(
        String licensePlate,
        String type,
        String brand,
        String model,
        double baseServiceFee,
        String ownerId,
        Integer seatingCapacity,
        Double cargoVolume,
        Integer engineCC
) {
}
