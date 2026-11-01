package com.autocare.controller;

import com.autocare.domain.Car;
import com.autocare.domain.Motorcycle;
import com.autocare.domain.Van;
import com.autocare.domain.Vehicle;
import com.autocare.dto.VehicleTypeInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Meta controller demonstrating live Dynamic Polymorphism.
 * Instantiates concrete Vehicle subclasses (Car, Van, Motorcycle) and calls getBaseServiceFee()
 * without instanceof or switch branching.
 */
@RestController
@RequestMapping("/api/v1/vehicle-types")
public class VehicleTypeController {

    @GetMapping
    public ResponseEntity<List<VehicleTypeInfo>> getVehicleTypes() {
        List<Vehicle> sampleVehicles = List.of(
                new Car("SAMPLE-CAR", "Sample", "Car", 5),
                new Van("SAMPLE-VAN", "Sample", "Van", 10.0),
                new Motorcycle("SAMPLE-MOTO", "Sample", "Motorcycle", 250)
        );

        List<VehicleTypeInfo> typeInfos = sampleVehicles.stream()
                .map(v -> new VehicleTypeInfo(v.getVehicleType().toUpperCase(), v.getBaseServiceFee()))
                .toList();

        return ResponseEntity.ok(typeInfos);
    }
}
