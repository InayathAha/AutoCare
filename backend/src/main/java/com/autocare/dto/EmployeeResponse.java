package com.autocare.dto;

/**
 * Response payload representing an employee.
 * monthlySalary demonstrates dynamic polymorphism: calculated by calling Employee.calculateSalary()
 * without instanceof or type branching.
 */
public record EmployeeResponse(
        String id,
        String type,
        String name,
        String phone,
        String email,
        double baseSalary,
        double monthlySalary,
        String specialization,
        Integer overtimeHours,
        Double hourlyRate,
        Double monthlyBonus
) {
}
