package com.autocare.service;

import com.autocare.domain.Car;
import com.autocare.domain.Customer;
import com.autocare.domain.Vehicle;
import com.autocare.dto.*;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.mapper.*;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.InvoiceRepository;
import com.autocare.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private final CustomerMapper customerMapper = new CustomerMapper();
    private final VehicleMapper vehicleMapper = new VehicleMapper();
    private final VehicleFactory vehicleFactory = new VehicleFactory();
    private final TaskMapper taskMapper = new TaskMapper();
    private final InvoiceMapper invoiceMapper = new InvoiceMapper(taskMapper);

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                customerRepository,
                vehicleRepository,
                invoiceRepository,
                customerMapper,
                vehicleMapper,
                vehicleFactory,
                invoiceMapper
        );
    }

    @Test
    @DisplayName("create() persists new customer when email is unique")
    void create_success() {
        CustomerRequest req = new CustomerRequest("Alice Smith", "0771234567", "alice@test.com");
        when(customerRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        CustomerResponse actual = customerService.create(req);

        assertThat(actual.name()).isEqualTo("Alice Smith");
        assertThat(actual.phone()).isEqualTo("0771234567");
        assertThat(actual.email()).isEqualTo("alice@test.com");
        assertThat(actual.loyaltyPoints()).isEqualTo(0);
        assertThat(actual.vehicleCount()).isEqualTo(0);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("create() throws DuplicateResourceException on duplicate email")
    void create_duplicateEmail_throws() {
        CustomerRequest req = new CustomerRequest("Alice Smith", "0771234567", "alice@test.com");
        when(customerRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("alice@test.com");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() updates customer details when valid")
    void update_success() {
        Customer existing = new Customer("c1", "Alice Old", "0771111111", "alice@test.com");
        when(customerRepository.findById("c1")).thenReturn(Optional.of(existing));

        CustomerRequest req = new CustomerRequest("Alice New", "0772222222", "alice.new@test.com");
        when(customerRepository.existsByEmail("alice.new@test.com")).thenReturn(false);

        CustomerResponse result = customerService.update("c1", req);

        assertThat(result.name()).isEqualTo("Alice New");
        assertThat(result.phone()).isEqualTo("0772222222");
        assertThat(result.email()).isEqualTo("alice.new@test.com");
        assertThat(existing.getName()).isEqualTo("Alice New");
        assertThat(existing.getPhone()).isEqualTo("0772222222");
        assertThat(existing.getEmail()).isEqualTo("alice.new@test.com");
    }

    @Test
    @DisplayName("update() throws DuplicateResourceException if updated email belongs to someone else")
    void update_duplicateEmail_throws() {
        Customer existing = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        when(customerRepository.findById("c1")).thenReturn(Optional.of(existing));

        CustomerRequest req = new CustomerRequest("Alice", "0771111111", "taken@test.com");
        when(customerRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update("c1", req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("delete() deletes customer when no registered vehicles and no invoices")
    void delete_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));
        when(invoiceRepository.findByCustomerIdOrderByIssuedAtDesc("c1")).thenReturn(Collections.emptyList());

        customerService.delete("c1");

        verify(customerRepository).delete(customer);
    }

    @Test
    @DisplayName("delete() throws DataIntegrityViolationException when customer has registered vehicles")
    void delete_withVehicles_throws() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Vehicle vehicle = new Car("CAB-1234", "Toyota", "Corolla", 5);
        customer.registerVehicle(vehicle);

        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.delete("c1"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("registered vehicles");

        verify(customerRepository, never()).delete(any());
    }

    @Test
    @DisplayName("get() returns mapped CustomerResponse or throws ResourceNotFoundException")
    void get_found_and_notFound() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));
        when(customerRepository.findById("unknown")).thenReturn(Optional.empty());

        CustomerResponse resp = customerService.get("c1");
        assertThat(resp.id()).isEqualTo("c1");
        assertThat(resp.name()).isEqualTo("Alice");

        assertThatThrownBy(() -> customerService.get("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("registerVehicle() associates vehicle through domain method")
    void registerVehicle_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));

        VehicleRequest req = new VehicleRequest("CAR", "CAB-1234", "Toyota", "Corolla", 5, null, null);
        when(vehicleRepository.existsById("CAB-1234")).thenReturn(false);

        VehicleResponse actual = customerService.registerVehicle("c1", req);

        assertThat(actual.licensePlate()).isEqualTo("CAB-1234");
        assertThat(actual.type()).isEqualTo("CAR");
        assertThat(actual.baseServiceFee()).isEqualTo(50.0);
        assertThat(actual.seatingCapacity()).isEqualTo(5);
        assertThat(customer.getVehicles()).hasSize(1);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("registerVehicle() throws DuplicateResourceException on existing plate")
    void registerVehicle_duplicatePlate_throws() {
        VehicleRequest req = new VehicleRequest("CAR", "CAB-1234", "Toyota", "Corolla", 5, null, null);
        when(vehicleRepository.existsById("CAB-1234")).thenReturn(true);

        assertThatThrownBy(() -> customerService.registerVehicle("c1", req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CAB-1234");
    }

    @Test
    @DisplayName("redeemPoints() deducts points using domain rule")
    void redeemPoints_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        customer.addLoyaltyPoints(100);
        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));

        RedeemResponse resp = customerService.redeemPoints("c1", 40);

        assertThat(resp.redeemed()).isTrue();
        assertThat(resp.remainingPoints()).isEqualTo(60);
        assertThat(customer.getLoyaltyPoints()).isEqualTo(60);
    }

    @Test
    @DisplayName("redeemPoints() throws BusinessRuleViolationException when balance is insufficient")
    void redeemPoints_insufficient_throws() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        customer.addLoyaltyPoints(20);
        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.redeemPoints("c1", 50))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Insufficient loyalty points");
    }
}
