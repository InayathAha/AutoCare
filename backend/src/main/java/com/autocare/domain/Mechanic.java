package com.autocare.domain;

import com.autocare.exception.BusinessRuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Demonstrates Inheritance, Encapsulation, and Dynamic Polymorphism:
 * Mechanic extends Employee and overrides calculateSalary() to incorporate overtime pay.
 * Contains domain logic to complete assigned ServiceTasks.
 */
@Entity
@DiscriminatorValue("MECHANIC")
public class Mechanic extends Employee {

    @Column(name = "specialization", length = 50)
    private String specialization;

    @Column(name = "overtime_hours")
    private int overtimeHours;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Mechanic() {
        super();
    }

    /**
     * Parameterized constructor for Mechanic.
     *
     * @param id             unique UUID identifier
     * @param name           full mechanic name
     * @param phone          contact telephone
     * @param email          email address
     * @param baseSalary     base monthly salary
     * @param specialization mechanical specialization (e.g., Engine, Transmission, Electrical)
     * @param hourlyRate     hourly rate for overtime calculation
     */
    public Mechanic(String id, String name, String phone, String email, double baseSalary,
                    String specialization, double hourlyRate) {
        super(id, name, phone, email, baseSalary);
        if (specialization == null || specialization.isBlank()) {
            throw new IllegalArgumentException("Specialization cannot be null or blank");
        }
        if (hourlyRate < 0) {
            throw new IllegalArgumentException("Hourly rate cannot be negative");
        }
        this.specialization = specialization.trim();
        this.hourlyRate = BigDecimal.valueOf(hourlyRate).setScale(2, RoundingMode.HALF_UP);
        this.overtimeHours = 0;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        if (specialization == null || specialization.isBlank()) {
            throw new IllegalArgumentException("Specialization cannot be null or blank");
        }
        this.specialization = specialization.trim();
    }

    public int getOvertimeHours() {
        return overtimeHours;
    }

    public double getHourlyRate() {
        return hourlyRate != null ? hourlyRate.doubleValue() : 0.0;
    }

    public void setHourlyRate(double hourlyRate) {
        if (hourlyRate < 0) {
            throw new IllegalArgumentException("Hourly rate cannot be negative");
        }
        this.hourlyRate = BigDecimal.valueOf(hourlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Logs additional overtime hours worked by this mechanic.
     * Invariant: hours must be strictly greater than 0.
     *
     * @param hours positive number of overtime hours
     * @throws IllegalArgumentException if hours <= 0
     */
    public void logOvertime(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Overtime hours must be greater than zero");
        }
        this.overtimeHours += hours;
    }

    /**
     * Resets logged overtime hours, typically at the end of a payroll period.
     */
    public void resetOvertime() {
        this.overtimeHours = 0;
    }

    /**
     * Completes a service task if and only if this mechanic is assigned to it.
     * Enforces the business rule that a mechanic cannot complete another mechanic's task.
     *
     * @param task service task to mark complete
     * @throws IllegalArgumentException       if task is null
     * @throws BusinessRuleViolationException if task is assigned to another mechanic
     */
    public void completeServiceTask(ServiceTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Service task cannot be null");
        }
        if (task.getAssignedMechanic() == null || !this.getId().equals(task.getAssignedMechanic().getId())) {
            throw new BusinessRuleViolationException(
                    "Mechanic " + getName() + " is not assigned to task " + task.getTaskId()
            );
        }
        task.markCompleted();
    }

    /**
     * Demonstrates dynamic polymorphism: overrides Employee.calculateSalary().
     * Calculates total salary as: baseSalary + (overtimeHours * hourlyRate).
     *
     * @return calculated monthly salary
     */
    @Override
    public double calculateSalary() {
        BigDecimal rate = this.hourlyRate != null ? this.hourlyRate : BigDecimal.ZERO;
        BigDecimal overtimePay = rate.multiply(BigDecimal.valueOf(overtimeHours));
        return this.baseSalary.add(overtimePay).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public String getEmployeeRole() {
        return "MECHANIC";
    }

    @Override
    public String describe() {
        return String.format(
                "Mechanic[id=%s, name=%s, role=%s, specialization=%s, baseSalary=%.2f, overtimeHours=%d, hourlyRate=%.2f, totalSalary=%.2f]",
                getId(), getName(), getEmployeeRole(), specialization, getBaseSalary(), overtimeHours, getHourlyRate(), calculateSalary()
        );
    }
}
