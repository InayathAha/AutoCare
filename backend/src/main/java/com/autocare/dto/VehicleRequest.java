package com.autocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Polymorphic request payload for registering a vehicle.
 */
public record VehicleRequest(
        @NotBlank(message = "Vehicle type is required (CAR, VAN, MOTORCYCLE)")
        String type,

        @NotBlank(message = "License plate is required")
        @Size(max = 20, message = "License plate must not exceed 20 characters")
        String licensePlate,

        @NotBlank(message = "Brand is required")
        @Size(max = 50, message = "Brand must not exceed 50 characters")
        String brand,

        @NotBlank(message = "Model is required")
        @Size(max = 50, message = "Model must not exceed 50 characters")
        String model,

        Integer seatingCapacity, // Required when type = CAR
        Double cargoVolume,      // Required when type = VAN
        Integer engineCC         // Required when type = MOTORCYCLE
) {
}
