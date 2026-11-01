package com.autocare.mapper;

import com.autocare.domain.*;
import com.autocare.dto.VehicleResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting polymorphic Vehicle entities to VehicleResponse DTOs.
 * Demonstrates dynamic polymorphism: getBaseServiceFee() is dynamically dispatched
 * without type checking. Subclass-specific properties (seatingCapacity, cargoVolume, engineCC)
 * are populated according to subclass type.
 */
@Component
public class VehicleMapper {

    public VehicleResponse toResponse(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        Integer seatingCapacity = vehicle instanceof Car c ? c.getSeatingCapacity() : null;
        Double cargoVolume = vehicle instanceof Van v ? v.getCargoVolume() : null;
        Integer engineCC = vehicle instanceof Motorcycle m ? m.getEngineCC() : null;

        return new VehicleResponse(
                vehicle.getLicensePlate(),
                vehicle.getVehicleType().toUpperCase(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getBaseServiceFee(), // Dynamic polymorphism
                vehicle.getOwner() != null ? vehicle.getOwner().getId() : null,
                seatingCapacity,
                cargoVolume,
                engineCC
        );
    }
}
