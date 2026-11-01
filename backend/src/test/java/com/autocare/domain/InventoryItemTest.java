package com.autocare.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventoryItem - Stock Invariant & Boundary Tests")
class InventoryItemTest {

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = new InventoryItem("FLT-01", "Oil Filter", 12.50, 20, 5);
    }

    @Test
    @DisplayName("Initial state correctly initialized with version and restock timestamp")
    void initialState() {
        assertThat(item.getPartCode()).isEqualTo("FLT-01");
        assertThat(item.getPartName()).isEqualTo("Oil Filter");
        assertThat(item.getUnitPrice()).isEqualTo(12.50);
        assertThat(item.getStockQuantity()).isEqualTo(20);
        assertThat(item.getReorderThreshold()).isEqualTo(5);
        assertThat(item.getLastRestockedAt()).isNotNull();
        assertThat(item.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("deductStock returns true and decrements stock when quantity is valid and available")
    void deductStockSuccess() {
        boolean success = item.deductStock(5);

        assertThat(success).isTrue();
        assertThat(item.getStockQuantity()).isEqualTo(15);

        // Deduct remaining exact stock down to zero
        boolean successToZero = item.deductStock(15);
        assertThat(successToZero).isTrue();
        assertThat(item.getStockQuantity()).isZero();
    }

    @Test
    @DisplayName("deductStock returns false and leaves stock unchanged when quantity exceeds stock (too large)")
    void deductStockTooLargeReturnsFalse() {
        boolean success = item.deductStock(21);

        assertThat(success).isFalse();
        assertThat(item.getStockQuantity()).isEqualTo(20);
    }

    @ParameterizedTest(name = "deductStock returns false for invalid non-positive quantity: {0}")
    @ValueSource(ints = {0, -1, -50})
    @DisplayName("deductStock returns false when quantity <= 0 without modifying stock")
    void deductStockNonPositiveReturnsFalse(int invalidQuantity) {
        boolean success = item.deductStock(invalidQuantity);

        assertThat(success).isFalse();
        assertThat(item.getStockQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("restock increments stock and updates lastRestockedAt timestamp")
    void restockSuccess() throws InterruptedException {
        Instant beforeRestock = item.getLastRestockedAt();
        Thread.sleep(10); // Guarantee distinct timestamp

        item.restock(10);

        assertThat(item.getStockQuantity()).isEqualTo(30);
        assertThat(item.getLastRestockedAt()).isAfterOrEqualTo(beforeRestock);
    }

    @ParameterizedTest(name = "restock rejects non-positive quantity: {0}")
    @ValueSource(ints = {0, -1, -25})
    @DisplayName("restock throws IllegalArgumentException when quantity <= 0")
    void restockNonPositiveRejected(int invalidQuantity) {
        assertThatThrownBy(() -> item.restock(invalidQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        assertThat(item.getStockQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("isLowStock boundary test: false above threshold, true at boundary (equal), true below")
    void isLowStockBoundaries() {
        // Threshold is 5. Stock starts at 20 -> not low stock
        assertThat(item.isLowStock()).isFalse();

        // Stock = 6 (above threshold)
        item.deductStock(14);
        assertThat(item.getStockQuantity()).isEqualTo(6);
        assertThat(item.isLowStock()).isFalse();

        // Stock = 5 (exactly equal to reorder threshold) -> must return TRUE
        item.deductStock(1);
        assertThat(item.getStockQuantity()).isEqualTo(5);
        assertThat(item.isLowStock()).as("Boundary condition: stock == threshold must be low stock").isTrue();

        // Stock = 4 (below threshold) -> must return TRUE
        item.deductStock(1);
        assertThat(item.getStockQuantity()).isEqualTo(4);
        assertThat(item.isLowStock()).isTrue();

        // Stock = 0 (empty) -> must return TRUE
        item.deductStock(4);
        assertThat(item.getStockQuantity()).isZero();
        assertThat(item.isLowStock()).isTrue();
    }

    @Test
    @DisplayName("Property-style randomised sequence test: stock quantity NEVER drops below zero")
    void propertyStyleStockNeverNegative() {
        InventoryItem testItem = new InventoryItem("PRT-PROP", "Random Test Part", 10.00, 10, 5);
        Random random = new Random(42); // fixed seed for determinism

        for (int i = 0; i < 1000; i++) {
            boolean isDeduct = random.nextBoolean();
            int amount = random.nextInt(50) - 10; // generates values between -10 and 39

            if (isDeduct) {
                testItem.deductStock(amount);
            } else {
                if (amount > 0) {
                    testItem.restock(amount);
                } else {
                    assertThatThrownBy(() -> testItem.restock(amount))
                            .isInstanceOf(IllegalArgumentException.class);
                }
            }

            // Invariant assertion after every single operation
            assertThat(testItem.getStockQuantity())
                    .as("Stock quantity invariant breached at iteration %d: value is %d", i, testItem.getStockQuantity())
                    .isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Constructor parameter validations reject invalid values")
    void constructorValidations() {
        assertThatThrownBy(() -> new InventoryItem("", "Part", 10.0, 5, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InventoryItem("P-1", "   ", 10.0, 5, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InventoryItem("P-1", "Part", -1.0, 5, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InventoryItem("P-1", "Part", 10.0, -1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InventoryItem("P-1", "Part", 10.0, 5, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Mutators enforce invariants on unit price and threshold")
    void mutatorsEnforceInvariants() {
        item.setUnitPrice(15.99);
        assertThat(item.getUnitPrice()).isEqualTo(15.99);
        assertThatThrownBy(() -> item.setUnitPrice(-0.01)).isInstanceOf(IllegalArgumentException.class);

        item.setReorderThreshold(10);
        assertThat(item.getReorderThreshold()).isEqualTo(10);
        assertThatThrownBy(() -> item.setReorderThreshold(-1)).isInstanceOf(IllegalArgumentException.class);

        item.setPartName("Premium Filter");
        assertThat(item.getPartName()).isEqualTo("Premium Filter");
        assertThatThrownBy(() -> item.setPartName("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
