package com.autocare.domain;

import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.InsufficientStockException;
import com.autocare.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ServiceTask & TaskUsedPart - Lifecycle, Consumption & Cost Tests")
class ServiceTaskTest {

    private Mechanic mechanic;
    private ServiceTask task;
    private InventoryItem oilFilter;

    @BeforeEach
    void setUp() {
        mechanic = new Mechanic("m-01", "Mike", "0771122334", "mike@autocare.com", 1000.0, "General", 15.0);
        task = new ServiceTask("t-01", "Periodic Service & Oil Change", mechanic);
        oilFilter = new InventoryItem("FLT-01", "Oil Filter", 8.00, 10, 2);
    }

    @ParameterizedTest(name = "Transition from {0} to {1} -> valid={2}")
    @CsvSource({
            "PENDING,     PENDING,     false",
            "PENDING,     IN_PROGRESS, true",
            "PENDING,     COMPLETED,   false",
            "IN_PROGRESS, PENDING,     false",
            "IN_PROGRESS, IN_PROGRESS, false",
            "IN_PROGRESS, COMPLETED,   true",
            "COMPLETED,   PENDING,     false",
            "COMPLETED,   IN_PROGRESS, false",
            "COMPLETED,   COMPLETED,   false"
    })
    @DisplayName("Transition matrix: exactly 2 forward moves allowed out of 9 state pairs")
    void stateTransitionMatrix(String initial, String target, boolean shouldSucceed) {
        // Setup initial status
        ServiceTask testTask = new ServiceTask("t-matrix", "State test", mechanic);
        if ("IN_PROGRESS".equals(initial)) {
            testTask.setStatus("IN_PROGRESS");
        } else if ("COMPLETED".equals(initial)) {
            testTask.setStatus("IN_PROGRESS");
            testTask.setStatus("COMPLETED");
        }

        if (shouldSucceed) {
            testTask.setStatus(target);
            assertThat(testTask.getStatus()).isEqualTo(target);
        } else {
            assertThatThrownBy(() -> testTask.setStatus(target))
                    .isInstanceOf(InvalidStateTransitionException.class);
            assertThat(testTask.getStatus()).isEqualTo(initial);
        }
    }

    @Test
    @DisplayName("addUsedPart deducts inventory stock and recomputes partsCost")
    void addUsedPartSuccess() {
        task.addUsedPart(oilFilter, 2);

        assertThat(oilFilter.getStockQuantity()).isEqualTo(8);
        assertThat(task.getPartsCost()).isEqualTo(16.00);
        assertThat(task.getTotalTaskCost()).isEqualTo(16.00);
        assertThat(task.getUsedParts()).hasSize(1);

        TaskUsedPart usedPart = task.getUsedParts().get(0);
        assertThat(usedPart.getQuantity()).isEqualTo(2);
        assertThat(usedPart.getUnitPrice()).isEqualTo(8.00);
        assertThat(usedPart.getSubtotal()).isEqualTo(16.00);
    }

    @Test
    @DisplayName("Stock-rollback check: insufficient stock throws InsufficientStockException without modifying stock")
    void addUsedPartInsufficientStockDoesNotAlterStock() {
        // oilFilter has stock 10; requesting 15
        assertThatThrownBy(() -> task.addUsedPart(oilFilter, 15))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        // Assert stock remains untouched
        assertThat(oilFilter.getStockQuantity()).isEqualTo(10);
        assertThat(task.getUsedParts()).isEmpty();
        assertThat(task.getPartsCost()).isZero();
    }

    @Test
    @DisplayName("Adding the same part twice merges quantity and updates subtotal while preserving single line item")
    void addSamePartTwiceMergesQuantity() {
        task.addUsedPart(oilFilter, 2);
        task.addUsedPart(oilFilter, 3);

        assertThat(oilFilter.getStockQuantity()).isEqualTo(5);
        assertThat(task.getUsedParts()).hasSize(1);

        TaskUsedPart merged = task.getUsedParts().get(0);
        assertThat(merged.getQuantity()).isEqualTo(5);
        assertThat(merged.getUnitPrice()).isEqualTo(8.00);
        assertThat(merged.getSubtotal()).isEqualTo(40.00);
        assertThat(task.getPartsCost()).isEqualTo(40.00);
    }

    @Test
    @DisplayName("Snapshot price retained: changing catalog unit price does not alter historical task used part price")
    void snapshotPriceRetainedAfterPriceChange() {
        task.addUsedPart(oilFilter, 2);
        assertThat(task.getPartsCost()).isEqualTo(16.00);

        // Catalog price increases to 25.00
        oilFilter.setUnitPrice(25.00);

        TaskUsedPart usedPart = task.getUsedParts().get(0);
        assertThat(usedPart.getUnitPrice()).as("Snapshot unit price must remain 8.00").isEqualTo(8.00);
        assertThat(usedPart.getSubtotal()).as("Subtotal must remain 16.00").isEqualTo(16.00);
        assertThat(task.getPartsCost()).as("Task parts cost must remain 16.00").isEqualTo(16.00);
    }

    @Test
    @DisplayName("Cost formula: totalTaskCost = partsCost + laborCharge (16.00 + 30.00 = 46.00)")
    void calculateTaskCostPartsAndLabor() {
        // 2 x 8.00 = 16.00 parts cost
        task.addUsedPart(oilFilter, 2);

        // 1.5 hours @ 20.00/h = 30.00 labor charge
        task.recordBillableHours(1.5, 20.00);

        assertThat(task.getPartsCost()).isEqualTo(16.00);
        assertThat(task.getBillableHours()).isEqualTo(1.50);
        assertThat(task.getLaborCharge()).isEqualTo(30.00);

        double total = task.calculateTaskCost();
        assertThat(total).isEqualTo(46.00);
        assertThat(task.getTotalTaskCost()).isEqualTo(46.00);
    }

    @Test
    @DisplayName("No mutations allowed after task is marked COMPLETED")
    void noMutationsAfterCompleted() {
        task.startTask();
        task.markCompleted();

        assertThat(task.getStatus()).isEqualTo("COMPLETED");
        assertThat(task.getCompletedAt()).isNotNull();

        // Mutating parts rejected
        assertThatThrownBy(() -> task.addUsedPart(oilFilter, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("completed");

        // Recording labor rejected
        assertThatThrownBy(() -> task.recordBillableHours(2.0, 25.0))
                .isInstanceOf(BusinessRuleViolationException.class);

        // Changing description rejected
        assertThatThrownBy(() -> task.setDescription("New description"))
                .isInstanceOf(BusinessRuleViolationException.class);

        // Changing mechanic rejected
        Mechanic another = new Mechanic("m-02", "Alex", "0770000000", "alex@autocare.com", 1200.0, "Electrical", 20.0);
        assertThatThrownBy(() -> task.setAssignedMechanic(another))
                .isInstanceOf(BusinessRuleViolationException.class);

        // Further state transitions rejected
        assertThatThrownBy(() -> task.setStatus("IN_PROGRESS"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("getUsedItems returns unmodifiable map of InventoryItem to quantity")
    void getUsedItemsMap() {
        InventoryItem sparkPlug = new InventoryItem("SPK-01", "Spark Plug", 5.00, 20, 4);

        task.addUsedPart(oilFilter, 2);
        task.addUsedPart(sparkPlug, 4);

        Map<InventoryItem, Integer> map = task.getUsedItems();

        assertThat(map).hasSize(2);
        assertThat(map.get(oilFilter)).isEqualTo(2);
        assertThat(map.get(sparkPlug)).isEqualTo(4);

        assertThatThrownBy(() -> map.put(oilFilter, 10))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
