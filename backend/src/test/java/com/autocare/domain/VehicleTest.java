package com.autocare.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Vehicle Hierarchy - Dynamic Polymorphism & Invariant Tests")
class VehicleTest {

    @Test
    @DisplayName("Vehicle class is strictly abstract and cannot be directly instantiated")
    void vehicleClassIsAbstract() {
        assertThat(Modifier.isAbstract(Vehicle.class.getModifiers()))
                .as("Vehicle must be declared abstract")
                .isTrue();
    }

    @Test
    @DisplayName("Dynamic polymorphism: getBaseServiceFee() through List<Vehicle> asserts 50, 85, 30 without instanceof")
    void polymorphicBaseServiceFees() {
        Customer customer = new Customer("c-1", "Dave", "0771234567", "dave@example.com");

        Vehicle car = new Car("WP-CAR-01", "Toyota", "Corolla", customer, 5);
        Vehicle van = new Van("WP-VAN-02", "Ford", "Transit", customer, 12.5);
        Vehicle moto = new Motorcycle("WP-MOTO-03", "Yamaha", "R1", customer, 1000);

        List<Vehicle> fleet = List.of(car, van, moto);

        // Assert individual fees from polymorphism
        assertThat(fleet.get(0).getBaseServiceFee()).isEqualTo(50.00);
        assertThat(fleet.get(1).getBaseServiceFee()).isEqualTo(85.00);
        assertThat(fleet.get(2).getBaseServiceFee()).isEqualTo(30.00);

        // Assert vehicle types
        assertThat(fleet.get(0).getVehicleType()).isEqualTo("Car");
        assertThat(fleet.get(1).getVehicleType()).isEqualTo("Van");
        assertThat(fleet.get(2).getVehicleType()).isEqualTo("Motorcycle");

        // Polymorphic accumulation
        double totalFees = fleet.stream().mapToDouble(Vehicle::getBaseServiceFee).sum();
        assertThat(totalFees).isEqualTo(165.00);
    }

    @Test
    @DisplayName("@PrePersist lifecycle method synchronizes baseServiceFee with polymorphic fee")
    void prePersistSynchronizesFeeField() {
        Customer customer = new Customer("c-2", "Jane", "0779998888", "jane@example.com");
        Car car = new Car("WP-CAR-99", "Honda", "Civic", customer, 5);
        Van van = new Van("WP-VAN-99", "Mercedes", "Sprinter", customer, 15.0);
        Motorcycle moto = new Motorcycle("WP-MOTO-99", "Honda", "CBR", customer, 600);

        car.prePersist();
        van.prePersist();
        moto.prePersist();

        // Check via getter/reflection that baseServiceFee equals 50, 85, 30
        assertThat(car.getBaseServiceFee()).isEqualTo(50.00);
        assertThat(van.getBaseServiceFee()).isEqualTo(85.00);
        assertThat(moto.getBaseServiceFee()).isEqualTo(30.00);
    }

    @ParameterizedTest(name = "Car rejects non-positive seating capacity: {0}")
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("Car constructor rejects non-positive seating capacity")
    void carRejectsNonPositiveSeats(int invalidSeats) {
        assertThatThrownBy(() -> new Car("CAR-1", "Toyota", "Yaris", invalidSeats))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @ParameterizedTest(name = "Van rejects non-positive cargo volume: {0}")
    @ValueSource(doubles = {0.0, -0.5, -10.0})
    @DisplayName("Van constructor rejects non-positive cargo volume")
    void vanRejectsNonPositiveVolume(double invalidVolume) {
        assertThatThrownBy(() -> new Van("VAN-1", "Ford", "Transit", invalidVolume))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @ParameterizedTest(name = "Motorcycle rejects non-positive engine CC: {0}")
    @ValueSource(ints = {0, -50, -250})
    @DisplayName("Motorcycle constructor rejects non-positive engine displacement")
    void motorcycleRejectsNonPositiveEngineCC(int invalidCC) {
        assertThatThrownBy(() -> new Motorcycle("BIKE-1", "Yamaha", "MT-07", invalidCC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("Vehicle mutators enforce positive invariant checks")
    void vehicleMutatorsEnforceInvariants() {
        Car car = new Car("CAR-2", "Toyota", "Camry", 5);
        Van van = new Van("VAN-2", "Renault", "Master", 10.0);
        Motorcycle moto = new Motorcycle("BIKE-2", "Suzuki", "Hayabusa", 1300);

        car.setSeatingCapacity(7);
        assertThat(car.getSeatingCapacity()).isEqualTo(7);
        assertThatThrownBy(() -> car.setSeatingCapacity(0)).isInstanceOf(IllegalArgumentException.class);

        van.setCargoVolume(14.0);
        assertThat(van.getCargoVolume()).isEqualTo(14.0);
        assertThatThrownBy(() -> van.setCargoVolume(-1.0)).isInstanceOf(IllegalArgumentException.class);

        moto.setEngineCC(1340);
        assertThat(moto.getEngineCC()).isEqualTo(1340);
        assertThatThrownBy(() -> moto.setEngineCC(-100)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank license plate, brand, or model are rejected")
    void blankVehicleDetailsRejected() {
        assertThatThrownBy(() -> new Car("", "Toyota", "Corolla", 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Car("CAR-3", "   ", "Corolla", 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Car("CAR-3", "Toyota", "  ", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("License plate is automatically normalized to trimmed uppercase")
    void licensePlateNormalized() {
        Car car = new Car("  wp-cab-9999  ", "Nissan", "Leaf", 5);
        assertThat(car.getLicensePlate()).isEqualTo("WP-CAB-9999");
    }
}
