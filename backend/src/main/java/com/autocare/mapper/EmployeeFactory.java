package com.autocare.mapper;

import com.autocare.domain.*;
import com.autocare.dto.EmployeeRequest;
import com.autocare.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory pattern demonstrating Polymorphism and Abstraction:
 * Instantiates the appropriate Employee subclass (Mechanic or Manager)
 * based on the incoming employee role.
 */
@Component
public class EmployeeFactory {

    /**
     * Creates a concrete Employee subclass instance with a newly generated UUID.
     *
     * @param request employee creation DTO
     * @return concrete Employee subclass instance (Mechanic or Manager)
     */
    public Employee createEmployee(EmployeeRequest request) {
        return createEmployee(UUID.randomUUID().toString(), request);
    }

    /**
     * Creates a concrete Employee subclass instance with an explicit identifier.
     *
     * @param id      unique employee UUID
     * @param request employee creation DTO
     * @return concrete Employee subclass instance (Mechanic or Manager)
     */
    public Employee createEmployee(String id, EmployeeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Employee request cannot be null");
        }
        String type = request.type() != null ? request.type().trim().toUpperCase() : "";

        return switch (type) {
            case "MECHANIC" -> {
                if (request.specialization() == null || request.specialization().isBlank()) {
                    throw new BusinessRuleViolationException("MECHANIC requires a specialization");
                }
                double rate = request.hourlyRate() != null ? request.hourlyRate() : 25.0;
                yield new Mechanic(id, request.name(), request.phone(), request.email(), request.baseSalary(),
                        request.specialization(), rate);
            }
            case "MANAGER" -> {
                double bonus = request.monthlyBonus() != null ? request.monthlyBonus() : 0.0;
                yield new Manager(id, request.name(), request.phone(), request.email(), request.baseSalary(), bonus);
            }
            default -> throw new BusinessRuleViolationException("Unsupported employee type: " + request.type() + ". Must be MECHANIC or MANAGER.");
        };
    }
}
