package com.autocare.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Demonstrates Abstraction and Inheritance:
 * Abstract entity representing garage staff, mapped with Single Table inheritance.
 * Subclasses (Mechanic, Manager) demonstrate dynamic polymorphism via calculateSalary().
 * <p>
 * Demonstrates Encapsulation and Money Handling:
 * baseSalary is stored internally as BigDecimal (scale 2, HALF_UP) while exposing double
 * in public method signatures per the UML specification.
 */
@Entity
@Table(name = "employees")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "employee_role", length = 30)
@AttributeOverride(name = "id", column = @Column(name = "employee_id", length = 36, nullable = false))
public abstract class Employee extends Person {

    @Column(name = "base_salary", precision = 10, scale = 2, nullable = false)
    protected BigDecimal baseSalary;

    @Column(name = "hired_at", nullable = false, updatable = false)
    protected Instant hiredAt = Instant.now();

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Employee() {
        super();
    }

    /**
     * Parameterized constructor validating base salary.
     *
     * @param id         unique UUID identifier
     * @param name       full staff name
     * @param phone      telephone number
     * @param email      email address
     * @param baseSalary monthly base salary (must be >= 0)
     */
    public Employee(String id, String name, String phone, String email, double baseSalary) {
        super(id, name, phone, email);
        if (baseSalary < 0) {
            throw new IllegalArgumentException("Base salary cannot be negative");
        }
        this.baseSalary = BigDecimal.valueOf(baseSalary).setScale(2, RoundingMode.HALF_UP);
        this.hiredAt = Instant.now();
    }

    /**
     * Returns base salary as double per the UML specification.
     *
     * @return monthly base salary
     */
    public double getBaseSalary() {
        return baseSalary.doubleValue();
    }

    /**
     * Updates base salary with validation.
     *
     * @param baseSalary new base salary (must be >= 0)
     */
    public void setBaseSalary(double baseSalary) {
        if (baseSalary < 0) {
            throw new IllegalArgumentException("Base salary cannot be negative");
        }
        this.baseSalary = BigDecimal.valueOf(baseSalary).setScale(2, RoundingMode.HALF_UP);
    }

    public Instant getHiredAt() {
        return hiredAt;
    }

    /**
     * Demonstrates dynamic polymorphism: each employee role calculates total remuneration
     * (e.g. including overtime for mechanics, or bonuses for managers).
     *
     * @return calculated monthly salary
     */
    public abstract double calculateSalary();

    /**
     * Demonstrates dynamic polymorphism: returns role discriminator string ("MECHANIC", "MANAGER").
     *
     * @return employee role name
     */
    public abstract String getEmployeeRole();

    /**
     * Demonstrates dynamic polymorphism: displays employee profile details.
     */
    @Override
    public void displayProfile() {
        System.out.println(describe());
    }
}
