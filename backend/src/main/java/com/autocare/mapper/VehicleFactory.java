package com.autocare.mapper;

import com.autocare.domain.*;
import com.autocare.dto.VehicleRequest;
import com.autocare.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

/**
 * Factory pattern demonstrating Static/Dynamic Polymorphism:
 * Instantiates the appropriate Vehicle subclass (Car, Van, Motorcycle)
 * based on the incoming polymorphic request payload.
 * Encapsulates subclass creation so services can interact purely through Vehicle references.
 */
@Component
public class VehicleFactory {

    /**
     * Creates a concrete Vehicle subclass instance.
     *
     * @param request vehicle creation DTO
     * @param owner   customer owning the vehicle
     * @return concrete Vehicle subclass instance (Car, Van, or Motorcycle)
     */
    public Vehicle createVehicle(VehicleRequest request, Customer owner) {
        if (request == null) {
            throw new IllegalArgumentException("Vehicle request cannot be null");
        }
        String type = request.type() != null ? request.type().trim().toUpperCase() : "";

        return switch (type) {
            case "CAR" -> {
                if (request.seatingCapacity() == null || request.seatingCapacity() <= 0) {
                    throw new BusinessRuleViolationException("CAR requires a strictly positive seatingCapacity");
                }
                yield new Car(request.licensePlate(), request.brand(), request.model(), owner, request.seatingCapacity());
            }
            case "VAN" -> {
                if (request.cargoVolume() == null || request.cargoVolume() <= 0) {
                    throw new BusinessRuleViolationException("VAN requires a strictly positive cargoVolume");
                }
                yield new Van(request.licensePlate(), request.brand(), request.model(), owner, request.cargoVolume());
            }
            case "MOTORCYCLE" -> {
                if (request.engineCC() == null || request.engineCC() <= 0) {
                    throw new BusinessRuleViolationException("MOTORCYCLE requires a strictly positive engineCC");
                }
                yield new Motorcycle(request.licensePlate(), request.brand(), request.model(), owner, request.engineCC());
            }
            default -> throw new BusinessRuleViolationException("Unsupported vehicle type: " + request.type() + ". Must be CAR, VAN, or MOTORCYCLE.");
        };
    }
}
