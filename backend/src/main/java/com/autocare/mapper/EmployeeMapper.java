package com.autocare.mapper;

import com.autocare.domain.*;
import com.autocare.dto.EmployeeResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting polymorphic Employee entities to EmployeeResponse DTOs.
 * Demonstrates dynamic polymorphism: employee.calculateSalary() is dispatched polymorphically.
 */
@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) {
            return null;
        }

        String specialization = null;
        Integer overtimeHours = null;
        Double hourlyRate = null;
        Double monthlyBonus = null;

        if (employee instanceof Mechanic m) {
            specialization = m.getSpecialization();
            overtimeHours = m.getOvertimeHours();
            hourlyRate = m.getHourlyRate();
        } else if (employee instanceof Manager mgr) {
            monthlyBonus = mgr.getMonthlyBonus();
        }

        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeRole(),
                employee.getName(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getBaseSalary(),
                employee.calculateSalary(), // Dynamic polymorphism
                specialization,
                overtimeHours,
                hourlyRate,
                monthlyBonus
        );
    }
}
