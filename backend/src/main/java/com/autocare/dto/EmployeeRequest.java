package com.autocare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Polymorphic request payload for hiring/updating an employee.
 */
public record EmployeeRequest(
        @NotBlank(message = "Employee type is required (MECHANIC or MANAGER)")
        String type,

        @NotBlank(message = "Employee name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @PositiveOrZero(message = "Base salary must be non-negative")
        double baseSalary,

        String specialization,   // Required for MECHANIC
        Double hourlyRate,       // Optional/default for MECHANIC
        Double monthlyBonus      // Required for MANAGER
) {
}
