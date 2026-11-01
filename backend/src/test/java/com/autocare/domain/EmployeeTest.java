package com.autocare.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Employee Hierarchy - Polymorphic Salary & Inheritance Tests")
class EmployeeTest {

    @Test
    @DisplayName("Employee class is strictly abstract and cannot be directly instantiated")
    void employeeClassIsAbstract() {
        assertThat(Modifier.isAbstract(Employee.class.getModifiers()))
                .as("Employee must declare abstract modifier")
                .isTrue();
    }

    @Test
    @DisplayName("Salary fixtures 1150 and 2300 via polymorphic List<Employee> iteration without instanceof")
    void polymorphicSalaryCalculationFixtures() {
        // Mechanic fixture: base 1000, 10 hours overtime at 15/hr -> 1000 + 150 = 1150.00
        Mechanic mechanic = new Mechanic("emp-1", "Bob Smith", "0770000001", "bob@autocare.com", 1000.00, "Engine", 15.00);
        mechanic.logOvertime(10);

        // Manager fixture: base 2000, monthly bonus 300 -> 2000 + 300 = 2300.00
        Manager manager = new Manager("emp-2", "Sarah Connor", "0770000002", "sarah@autocare.com", 2000.00, 300.00);

        // Pure dynamic polymorphism: iterating via List<Employee>
        List<Employee> staff = List.of(mechanic, manager);

        assertThat(staff.get(0).calculateSalary()).isEqualTo(1150.00);
        assertThat(staff.get(1).calculateSalary()).isEqualTo(2300.00);

        double totalPayroll = staff.stream().mapToDouble(Employee::calculateSalary).sum();
        assertThat(totalPayroll).isEqualTo(3450.00);
    }

    @Test
    @DisplayName("Negative base salary in constructor throws IllegalArgumentException")
    void negativeBaseSalaryRejected() {
        assertThatThrownBy(() -> new Mechanic("emp-3", "Dave", "0770000003", "dave@autocare.com", -500.00, "Brakes", 20.00))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("Negative base salary in mutator throws IllegalArgumentException")
    void negativeBaseSalaryInSetterRejected() {
        Mechanic mechanic = new Mechanic("emp-3", "Dave", "0770000003", "dave@autocare.com", 1000.00, "Brakes", 20.00);

        assertThatThrownBy(() -> mechanic.setBaseSalary(-10.00))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
