package com.autocare.domain;

import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Mechanic Domain Entity Tests")
class MechanicTest {

    private Mechanic mechanic;

    @BeforeEach
    void setUp() {
        mechanic = new Mechanic("m-01", "Mike Miller", "0771122334", "mike@autocare.com", 1000.00, "Transmission", 15.00);
    }

    @Test
    @DisplayName("Mechanic salary fixture: base 1000 + 10h overtime @ 15 = 1150.00")
    void mechanicSalaryFixture() {
        mechanic.logOvertime(10);
        assertThat(mechanic.calculateSalary()).isEqualTo(1150.00);
    }

    @Test
    @DisplayName("Initial overtime is zero and salary equals base salary")
    void initialOvertimeIsZero() {
        assertThat(mechanic.getOvertimeHours()).isZero();
        assertThat(mechanic.calculateSalary()).isEqualTo(1000.00);
        assertThat(mechanic.getEmployeeRole()).isEqualTo("MECHANIC");
    }

    @Test
    @DisplayName("logOvertime increments overtime hours cumulatively")
    void logOvertimeSuccess() {
        mechanic.logOvertime(4);
        mechanic.logOvertime(6);
        assertThat(mechanic.getOvertimeHours()).isEqualTo(10);
    }

    @ParameterizedTest(name = "logOvertime with {0} hours throws IllegalArgumentException")
    @ValueSource(ints = {0, -1, -8})
    @DisplayName("logOvertime rejects zero and negative values (<= 0)")
    void logOvertimeRejectsZeroOrNegative(int invalidHours) {
        assertThatThrownBy(() -> mechanic.logOvertime(invalidHours))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("resetOvertime resets accumulated hours to zero")
    void resetOvertime() {
        mechanic.logOvertime(5);
        mechanic.resetOvertime();
        assertThat(mechanic.getOvertimeHours()).isZero();
        assertThat(mechanic.calculateSalary()).isEqualTo(1000.00);
    }

    @Test
    @DisplayName("completeServiceTask marks task COMPLETED when assigned to this mechanic")
    void completeServiceTaskSuccess() {
        ServiceTask task = new ServiceTask("t-01", "Replace oil filter", mechanic);
        task.startTask();

        mechanic.completeServiceTask(task);

        assertThat(task.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("completeServiceTask throws InvalidStateTransitionException if task is still PENDING")
    void completeServiceTaskRejectsPendingTask() {
        ServiceTask task = new ServiceTask("t-01", "Replace oil filter", mechanic);

        assertThatThrownBy(() -> mechanic.completeServiceTask(task))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("completeServiceTask throws BusinessRuleViolationException when assigned to another mechanic")
    void completeServiceTaskRejectsTaskAssignedToAnother() {
        Mechanic other = new Mechanic("m-02", "Other Mechanic", "0779988776", "other@autocare.com", 1200.00, "Engine", 18.00);
        ServiceTask task = new ServiceTask("t-02", "Brake pad replacement", other);
        task.startTask();

        assertThatThrownBy(() -> mechanic.completeServiceTask(task))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not assigned to task");
    }

    @Test
    @DisplayName("completeServiceTask rejects null task")
    void completeServiceTaskRejectsNull() {
        assertThatThrownBy(() -> mechanic.completeServiceTask(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Constructor rejects blank specialization or negative hourly rate")
    void constructorValidation() {
        assertThatThrownBy(() -> new Mechanic("m-03", "Test", "0770000000", "test@autocare.com", 1000.0, "", 15.0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Mechanic("m-03", "Test", "0770000000", "test@autocare.com", 1000.0, "Engine", -5.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
