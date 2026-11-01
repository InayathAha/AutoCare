package com.autocare.repository;

import com.autocare.domain.Employee;
import com.autocare.domain.Manager;
import com.autocare.domain.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Employee hierarchy (Mechanic, Manager).
 * Supports typed queries on subclasses as well as polymorphic queries on base Employee.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    /**
     * Finds mechanics by specialization (case-insensitive).
     * Demonstrates typed query on a specific polymorphic subclass.
     *
     * @param specialization specialization field (e.g. "Engine", "Brakes", "Diagnostics")
     * @return list of matching Mechanics
     */
    @Query("SELECT m FROM Mechanic m WHERE LOWER(m.specialization) = LOWER(:specialization)")
    List<Mechanic> findMechanicsBySpecialization(@Param("specialization") String specialization);

    /**
     * Finds all mechanics.
     *
     * @return list of all Mechanics
     */
    @Query("SELECT m FROM Mechanic m")
    List<Mechanic> findAllMechanics();

    /**
     * Finds all managers.
     *
     * @return list of all Managers
     */
    @Query("SELECT m FROM Manager m")
    List<Manager> findAllManagers();

    /**
     * Checks if an employee exists with the given email.
     *
     * @param email employee email
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
}
