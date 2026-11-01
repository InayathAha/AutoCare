package com.autocare.service;

import com.autocare.domain.Manager;
import com.autocare.domain.Mechanic;
import com.autocare.dto.EmployeeRequest;
import com.autocare.dto.EmployeeResponse;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.mapper.EmployeeFactory;
import com.autocare.mapper.EmployeeMapper;
import com.autocare.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private final EmployeeFactory employeeFactory = new EmployeeFactory();
    private final EmployeeMapper employeeMapper = new EmployeeMapper();

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, employeeFactory, employeeMapper);
    }

    @Test
    @DisplayName("create() persists Mechanic or Manager via EmployeeFactory")
    void create_success() {
        EmployeeRequest req = new EmployeeRequest("MECHANIC", "Bob Engine", "0771234567", "bob@test.com", 1000.0, "Engine", 15.0, null);
        when(employeeRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(employeeRepository.save(any(Mechanic.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeResponse actual = employeeService.create(req);

        assertThat(actual.name()).isEqualTo("Bob Engine");
        assertThat(actual.type()).isEqualTo("MECHANIC");
        assertThat(actual.baseSalary()).isEqualTo(1000.0);
        assertThat(actual.monthlySalary()).isEqualTo(1000.0);
        assertThat(actual.specialization()).isEqualTo("Engine");
        assertThat(actual.hourlyRate()).isEqualTo(15.0);
        verify(employeeRepository).save(any(Mechanic.class));
    }

    @Test
    @DisplayName("create() throws DuplicateResourceException on duplicate email")
    void create_duplicateEmail_throws() {
        EmployeeRequest req = new EmployeeRequest("MECHANIC", "Bob", "0771234567", "bob@test.com", 1000.0, "Engine", 15.0, null);
        when(employeeRepository.existsByEmail("bob@test.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("bob@test.com");
    }

    @Test
    @DisplayName("update() updates Mechanic specialization and hourly rate")
    void update_mechanic_success() {
        Mechanic mechanic = new Mechanic("m1", "Bob Old", "0771111111", "bob@test.com", 1000.0, "Brakes", 12.0);
        when(employeeRepository.findById("m1")).thenReturn(Optional.of(mechanic));
        when(employeeRepository.existsByEmail("bob.new@test.com")).thenReturn(false);

        EmployeeRequest req = new EmployeeRequest("MECHANIC", "Bob New", "0772222222", "bob.new@test.com", 1200.0, "Engine", 18.0, null);

        EmployeeResponse actual = employeeService.update("m1", req);

        assertThat(actual.name()).isEqualTo("Bob New");
        assertThat(actual.phone()).isEqualTo("0772222222");
        assertThat(actual.email()).isEqualTo("bob.new@test.com");
        assertThat(actual.baseSalary()).isEqualTo(1200.0);
        assertThat(actual.specialization()).isEqualTo("Engine");
        assertThat(actual.hourlyRate()).isEqualTo(18.0);
        assertThat(mechanic.getName()).isEqualTo("Bob New");
        assertThat(mechanic.getSpecialization()).isEqualTo("Engine");
        assertThat(mechanic.getHourlyRate()).isEqualTo(18.0);
    }

    @Test
    @DisplayName("update() throws BusinessRuleViolationException when attempting to change employee role")
    void update_changeRole_throws() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0771111111", "bob@test.com", 1000.0, "Engine", 15.0);
        when(employeeRepository.findById("m1")).thenReturn(Optional.of(mechanic));

        EmployeeRequest req = new EmployeeRequest("MANAGER", "Bob", "0771111111", "bob@test.com", 1000.0, null, null, 200.0);

        assertThatThrownBy(() -> employeeService.update("m1", req))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("role cannot be modified");
    }

    @Test
    @DisplayName("logOvertime() increments mechanic overtime hours and recalculates salary polymorphically")
    void logOvertime_success() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0771111111", "bob@test.com", 1000.0, "Engine", 15.0);
        when(employeeRepository.findById("m1")).thenReturn(Optional.of(mechanic));

        EmployeeResponse result = employeeService.logOvertime("m1", 5);

        assertThat(result.overtimeHours()).isEqualTo(5);
        assertThat(result.monthlySalary()).isEqualTo(1075.0); // 1000 + (5 * 15)
        assertThat(mechanic.getOvertimeHours()).isEqualTo(5);
        assertThat(mechanic.calculateSalary()).isEqualTo(1075.0);
    }

    @Test
    @DisplayName("logOvertime() throws BusinessRuleViolationException if employee is not a mechanic")
    void logOvertime_onManager_throws() {
        Manager manager = new Manager("mgr1", "Carol", "0773333333", "carol@test.com", 2000.0, 300.0);
        when(employeeRepository.findById("mgr1")).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> employeeService.logOvertime("mgr1", 5))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not a Mechanic");
    }

    @Test
    @DisplayName("list() delegates to appropriate repository queries based on role and specialization")
    void list_filters() {
        Mechanic m1 = new Mechanic("m1", "Bob", "0771", "b@t.com", 1000.0, "Engine", 15.0);
        when(employeeRepository.findMechanicsBySpecialization("Engine")).thenReturn(List.of(m1));

        List<EmployeeResponse> res = employeeService.list("MECHANIC", "Engine");
        assertThat(res).hasSize(1);
        assertThat(res.get(0).name()).isEqualTo("Bob");
        verify(employeeRepository).findMechanicsBySpecialization("Engine");
    }
}
