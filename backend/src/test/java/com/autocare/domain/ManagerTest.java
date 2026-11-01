package com.autocare.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Manager Domain Entity Tests")
class ManagerTest {

    private Manager manager;

    @BeforeEach
    void setUp() {
        manager = new Manager("mgr-01", "Emma Watson", "0775566778", "emma@autocare.com", 2000.00, 300.00);
    }

    @Test
    @DisplayName("Manager salary fixture: base 2000 + monthly bonus 300 = 2300.00")
    void managerSalaryFixture() {
        assertThat(manager.calculateSalary()).isEqualTo(2300.00);
        assertThat(manager.getMonthlyBonus()).isEqualTo(300.00);
        assertThat(manager.getEmployeeRole()).isEqualTo("MANAGER");
    }

    @Test
    @DisplayName("setMonthlyBonus updates bonus and recalculates salary")
    void updateMonthlyBonus() {
        manager.setMonthlyBonus(500.00);
        assertThat(manager.getMonthlyBonus()).isEqualTo(500.00);
        assertThat(manager.calculateSalary()).isEqualTo(2500.00);
    }

    @ParameterizedTest(name = "setMonthlyBonus with {0} throws IllegalArgumentException")
    @ValueSource(doubles = {-0.01, -100.00})
    @DisplayName("setMonthlyBonus rejects negative amounts")
    void setMonthlyBonusRejectsNegative(double negativeBonus) {
        assertThatThrownBy(() -> manager.setMonthlyBonus(negativeBonus))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("Constructor rejects negative bonus")
    void constructorRejectsNegativeBonus() {
        assertThatThrownBy(() -> new Manager("mgr-02", "Alex", "0770000000", "alex@autocare.com", 2000.00, -50.00))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("approveDiscount delegates to invoice.applyDiscount(rate)")
    void approveDiscountDelegatesToInvoice() {
        Customer customer = new Customer("c-1", "Bob", "0771234567", "bob@example.com");
        Car vehicle = new Car("ABC-1111", "Toyota", "Prius", customer, 5); // base fee 50.00
        Invoice invoice = new Invoice("inv-01", customer, vehicle);

        manager.approveDiscount(invoice, 15.0); // 15% of 50.00 = 7.50

        assertThat(invoice.getDiscountAmount()).isEqualTo(7.50);
    }

    @Test
    @DisplayName("approveDiscount rejects null invoice")
    void approveDiscountRejectsNull() {
        assertThatThrownBy(() -> manager.approveDiscount(null, 10.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("generateInventoryReport formats low stock items accurately")
    void generateInventoryReportWithLowStock() {
        InventoryItem healthy = new InventoryItem("OIL-01", "Engine Oil 5W30", 25.0, 100, 20);
        InventoryItem low1 = new InventoryItem("FLT-02", "Oil Filter", 8.0, 4, 10);
        InventoryItem low2 = new InventoryItem("BRK-03", "Brake Pads Front", 45.0, 2, 5);

        String report = manager.generateInventoryReport(List.of(healthy, low1, low2));

        assertThat(report)
                .contains("LOW STOCK INVENTORY REPORT")
                .contains("Emma Watson")
                .contains("FLT-02")
                .contains("Oil Filter")
                .contains("BRK-03")
                .doesNotContain("OIL-01");
    }

    @Test
    @DisplayName("generateInventoryReport indicates healthy status when no items are low on stock")
    void generateInventoryReportAllHealthy() {
        InventoryItem healthy = new InventoryItem("OIL-01", "Engine Oil 5W30", 25.0, 100, 20);

        String report = manager.generateInventoryReport(List.of(healthy));

        assertThat(report).contains("All inventory items currently above reorder thresholds");
    }

    @Test
    @DisplayName("generateInventoryReport with empty list returns clean header message")
    void generateInventoryReportEmpty() {
        String report = manager.generateInventoryReport();

        assertThat(report).contains("No items provided or all stock levels healthy");
    }
}
