package com.autocare.domain;

import com.autocare.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Customer Domain Entity Tests")
class CustomerTest {

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer("c-001", "John Doe", "0771234567", "john.doe@example.com");
    }

    @Test
    @DisplayName("Initial customer state has zero points and empty vehicle list")
    void initialState() {
        assertThat(customer.getLoyaltyPoints()).isZero();
        assertThat(customer.getVehicles()).isEmpty();
    }

    @Test
    @DisplayName("addLoyaltyPoints increments points for positive values")
    void addLoyaltyPointsSuccess() {
        customer.addLoyaltyPoints(50);
        assertThat(customer.getLoyaltyPoints()).isEqualTo(50);

        customer.addLoyaltyPoints(30);
        assertThat(customer.getLoyaltyPoints()).isEqualTo(80);
    }

    @ParameterizedTest(name = "addLoyaltyPoints with {0} points throws IllegalArgumentException")
    @ValueSource(ints = {0, -1, -50})
    @DisplayName("addLoyaltyPoints rejects zero and negative points")
    void addLoyaltyPointsRejectsZeroOrNegative(int invalidPoints) {
        assertThatThrownBy(() -> customer.addLoyaltyPoints(invalidPoints))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("redeemPoints succeeds and decrements points when balance is sufficient (true)")
    void redeemPointsSuccess() {
        customer.addLoyaltyPoints(100);

        boolean result = customer.redeemPoints(40);

        assertThat(result).isTrue();
        assertThat(customer.getLoyaltyPoints()).isEqualTo(60);
    }

    @Test
    @DisplayName("redeemPoints returns false when requested points exceed available balance (false)")
    void redeemPointsExceedsBalanceReturnsFalse() {
        customer.addLoyaltyPoints(50);

        boolean result = customer.redeemPoints(60);

        assertThat(result).isFalse();
        assertThat(customer.getLoyaltyPoints()).isEqualTo(50);
    }

    @ParameterizedTest(name = "redeemPoints with invalid points {0} returns false without mutating state")
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("redeemPoints returns false for non-positive / invalid point values (invalid)")
    void redeemPointsInvalidValuesReturnFalse(int invalidPoints) {
        customer.addLoyaltyPoints(50);

        boolean result = customer.redeemPoints(invalidPoints);

        assertThat(result).isFalse();
        assertThat(customer.getLoyaltyPoints()).isEqualTo(50);
    }

    @Test
    @DisplayName("registerVehicle sets owner and appends to vehicle list")
    void registerVehicleSuccess() {
        Car v1 = new Car("CAB-1234", "Toyota", "Corolla", 5);

        customer.registerVehicle(v1);

        assertThat(customer.getVehicles()).hasSize(1);
        assertThat(customer.getVehicles().get(0).getLicensePlate()).isEqualTo("CAB-1234");
        assertThat(v1.getOwner()).isEqualTo(customer);
    }

    @Test
    @DisplayName("registerVehicle rejects duplicate license plate with DuplicateResourceException")
    void registerVehicleRejectsDuplicates() {
        Car v1 = new Car("WP-CAB-1234", "Toyota", "Corolla", 5);
        Car v2 = new Car("wp-cab-1234", "Toyota", "Axio", 5);

        customer.registerVehicle(v1);

        assertThatThrownBy(() -> customer.registerVehicle(v2))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("registerVehicle rejects null vehicle")
    void registerVehicleRejectsNull() {
        assertThatThrownBy(() -> customer.registerVehicle(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getVehicles returns unmodifiable list to enforce encapsulation")
    void getVehiclesIsUnmodifiable() {
        Car v1 = new Car("WP-CAB-1234", "Toyota", "Corolla", 5);
        customer.registerVehicle(v1);

        List<Vehicle> list = customer.getVehicles();
        assertThatThrownBy(() -> list.add(new Car("WP-XYZ-9999", "Honda", "Civic", 5)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("displayProfile outputs formatted customer summary")
    void describeAndDisplayProfile() {
        customer.addLoyaltyPoints(25);
        String summary = customer.describe();

        assertThat(summary)
                .contains("John Doe")
                .contains("0771234567")
                .contains("john.doe@example.com")
                .contains("loyaltyPoints=25");
    }
}
