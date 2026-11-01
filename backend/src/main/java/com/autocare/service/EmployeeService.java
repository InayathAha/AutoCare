package com.autocare.service;

import com.autocare.domain.Employee;
import com.autocare.domain.Manager;
import com.autocare.domain.Mechanic;
import com.autocare.dto.EmployeeRequest;
import com.autocare.dto.EmployeeResponse;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.mapper.EmployeeFactory;
import com.autocare.mapper.EmployeeMapper;
import com.autocare.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service orchestrating employee administration and dispatching use cases.
 * Demonstrates dynamic polymorphism: employee calculations and salary inquiries
 * dispatch polymorphically without type branching in business logic.
 */
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFactory employeeFactory;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeFactory employeeFactory,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeFactory = employeeFactory;
        this.employeeMapper = employeeMapper;
    }

    public EmployeeResponse create(EmployeeRequest req) {
        String email = req.email().trim();
        if (employeeRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An employee with email " + email + " already exists");
        }
        Employee employee = employeeFactory.createEmployee(req);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    public EmployeeResponse update(String id, EmployeeRequest req) {
        Employee employee = findEmployeeOrThrow(id);
        String requestedRole = req.type() != null ? req.type().trim().toUpperCase() : "";
        if (!employee.getEmployeeRole().equalsIgnoreCase(requestedRole)) {
            throw new BusinessRuleViolationException("Employee role cannot be modified after creation");
        }

        String newEmail = req.email().trim();
        if (!employee.getEmail().equalsIgnoreCase(newEmail) && employeeRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("An employee with email " + newEmail + " already exists");
        }

        employee.setName(req.name());
        employee.setPhone(req.phone());
        employee.setEmail(newEmail);
        employee.setBaseSalary(req.baseSalary());

        if (employee instanceof Mechanic mechanic) {
            if (req.specialization() != null && !req.specialization().isBlank()) {
                mechanic.setSpecialization(req.specialization());
            }
            if (req.hourlyRate() != null) {
                mechanic.setHourlyRate(req.hourlyRate());
            }
        } else if (employee instanceof Manager manager) {
            if (req.monthlyBonus() != null) {
                manager.setMonthlyBonus(req.monthlyBonus());
            }
        }

        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(String id) {
        return employeeMapper.toResponse(findEmployeeOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(String role, String specialization) {
        List<? extends Employee> list;

        if (role != null && !role.isBlank()) {
            String cleanRole = role.trim().toUpperCase();
            if ("MECHANIC".equals(cleanRole)) {
                if (specialization != null && !specialization.isBlank()) {
                    list = employeeRepository.findMechanicsBySpecialization(specialization.trim());
                } else {
                    list = employeeRepository.findAllMechanics();
                }
            } else if ("MANAGER".equals(cleanRole)) {
                list = employeeRepository.findAllManagers();
            } else {
                throw new BusinessRuleViolationException("Unknown employee role filter: " + role);
            }
        } else {
            list = employeeRepository.findAll();
        }

        return list.stream().map(employeeMapper::toResponse).toList();
    }

    public EmployeeResponse logOvertime(String mechanicId, int hours) {
        Employee employee = findEmployeeOrThrow(mechanicId);
        if (!(employee instanceof Mechanic mechanic)) {
            throw new BusinessRuleViolationException("Cannot log overtime: employee " + mechanicId + " is not a Mechanic");
        }
        mechanic.logOvertime(hours); // Invariant validation in domain model
        return employeeMapper.toResponse(mechanic);
    }

    private Employee findEmployeeOrThrow(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
}
