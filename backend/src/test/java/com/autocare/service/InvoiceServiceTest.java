package com.autocare.service;

import com.autocare.config.BillingProperties;
import com.autocare.domain.*;
import com.autocare.dto.*;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.InvalidDiscountException;
import com.autocare.mapper.InvoiceMapper;
import com.autocare.mapper.TaskMapper;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.EmployeeRepository;
import com.autocare.repository.InvoiceRepository;
import com.autocare.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Unit Tests")
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private final TaskMapper taskMapper = new TaskMapper();
    private final InvoiceMapper invoiceMapper = new InvoiceMapper(taskMapper);

    private BillingPolicy billingPolicy;
    private BillingProperties billingProperties;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        billingPolicy = BillingPolicy.defaultPolicy();
        billingProperties = new BillingProperties();
        invoiceService = new InvoiceService(
                invoiceRepository,
                customerRepository,
                vehicleRepository,
                employeeRepository,
                invoiceMapper,
                billingPolicy,
                billingProperties
        );
    }

    @Test
    @DisplayName("open() creates invoice when vehicle belongs to customer")
    void open_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        customer.registerVehicle(car);

        when(customerRepository.findById("c1")).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById("CAB-1234")).thenReturn(Optional.of(car));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        OpenInvoiceRequest req = new OpenInvoiceRequest("c1", "CAB-1234");
        InvoiceResponse actual = invoiceService.open(req);

        assertThat(actual.customerId()).isEqualTo("c1");
        assertThat(actual.licensePlate()).isEqualTo("CAB-1234");
        assertThat(actual.vehicleType()).isEqualTo("CAR");
        assertThat(actual.baseServiceFee()).isEqualTo(50.0);
        assertThat(actual.status()).isEqualTo("OPEN");
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("open() throws BusinessRuleViolationException when vehicle belongs to another customer")
    void open_wrongOwner_throws() {
        Customer alice = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Customer bob = new Customer("c2", "Bob", "0772222222", "bob@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        bob.registerVehicle(car); // owned by Bob, not Alice

        when(customerRepository.findById("c1")).thenReturn(Optional.of(alice));
        when(vehicleRepository.findById("CAB-1234")).thenReturn(Optional.of(car));

        OpenInvoiceRequest req = new OpenInvoiceRequest("c1", "CAB-1234");
        assertThatThrownBy(() -> invoiceService.open(req))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not registered to customer");
    }

    @Test
    @DisplayName("applyPercentageDiscount() unapproved <= 10% succeeds without managerId")
    void applyPercentageDiscount_unapproved_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car, 8.0, billingPolicy);

        when(invoiceRepository.findByIdWithTasksAndDetails("inv1")).thenReturn(Optional.of(invoice));

        DiscountPercentageRequest req = new DiscountPercentageRequest(10.0, null);
        InvoiceResponse response = invoiceService.applyPercentageDiscount("inv1", req);

        // Subtotal = 50.00, 10% discount = 5.00
        assertThat(invoice.getDiscountAmount()).isEqualTo(5.00);
        assertThat(response.discountAmount()).isEqualTo(5.00);
    }

    @Test
    @DisplayName("applyPercentageDiscount() > 10% without managerId throws InvalidDiscountException")
    void applyPercentageDiscount_aboveCapWithoutManager_throws() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car, 8.0, billingPolicy);

        when(invoiceRepository.findByIdWithTasksAndDetails("inv1")).thenReturn(Optional.of(invoice));

        DiscountPercentageRequest req = new DiscountPercentageRequest(15.0, null);
        assertThatThrownBy(() -> invoiceService.applyPercentageDiscount("inv1", req))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("manager approval required");
    }

    @Test
    @DisplayName("applyPercentageDiscount() > 10% with manager approval succeeds")
    void applyPercentageDiscount_withManager_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car, 8.0, billingPolicy);

        when(invoiceRepository.findByIdWithTasksAndDetails("inv1")).thenReturn(Optional.of(invoice));

        Manager manager = new Manager("mgr1", "Carol", "0773333333", "carol@test.com", 2000.0, 300.0);
        when(employeeRepository.findById("mgr1")).thenReturn(Optional.of(manager));

        DiscountPercentageRequest req = new DiscountPercentageRequest(20.0, "mgr1");
        InvoiceResponse response = invoiceService.applyPercentageDiscount("inv1", req);

        // Subtotal = 50.00, 20% discount = 10.00
        assertThat(invoice.getDiscountAmount()).isEqualTo(10.00);
        assertThat(response.discountAmount()).isEqualTo(10.00);
    }

    @Test
    @DisplayName("applyCouponDiscount() succeeds with configured coupon code")
    void applyCouponDiscount_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car, 8.0, billingPolicy);

        when(invoiceRepository.findByIdWithTasksAndDetails("inv1")).thenReturn(Optional.of(invoice));

        DiscountCouponRequest req = new DiscountCouponRequest("SAVE10", 10.00);
        InvoiceResponse response = invoiceService.applyCouponDiscount("inv1", req);

        assertThat(invoice.getDiscountAmount()).isEqualTo(10.00);
        assertThat(response.discountAmount()).isEqualTo(10.00);
    }

    @Test
    @DisplayName("issue() freezes totals and requires all tasks COMPLETED")
    void issue_success_and_invariants() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car, 8.0, billingPolicy);

        when(invoiceRepository.findByIdWithTasksAndDetails("inv1")).thenReturn(Optional.of(invoice));

        // Invoice with no tasks throws
        assertThatThrownBy(() -> invoiceService.issue("inv1"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no service tasks");

        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Fix Brakes", mechanic);
        invoice.addTask(task);

        // Task still PENDING throws
        assertThatThrownBy(() -> invoiceService.issue("inv1"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("must be COMPLETED");

        task.startTask();
        mechanic.completeServiceTask(task);

        InvoiceResponse response = invoiceService.issue("inv1");
        assertThat(invoice.getStatus()).isEqualTo("ISSUED");
        assertThat(response.status()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("settle() transitions ISSUED to PAID and awards loyalty points")
    void settle_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car, 0.0, billingPolicy);

        Mechanic mechanic = new Mechanic("m1", "Bob", "0772222222", "bob@test.com", 1000.0, "Engine", 15.0);
        ServiceTask task = new ServiceTask("t1", "Fix Brakes", mechanic);
        task.recordBillableHours(5.0, 10.0); // 50.00 labor
        task.startTask();
        mechanic.completeServiceTask(task);
        invoice.addTask(task);
        invoice.issue(); // Subtotal: 50 base + 50 task = 100.00. Tax = 0. Total = 100.00

        when(invoiceRepository.findByIdWithTasksAndDetails("inv1")).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.settle("inv1");

        assertThat(invoice.getStatus()).isEqualTo("PAID");
        assertThat(response.status()).isEqualTo("PAID");
        // 100.00 * 0.1 = 10 loyalty points awarded
        assertThat(customer.getLoyaltyPoints()).isEqualTo(10);
    }

    @Test
    @DisplayName("receipt() generates printable text receipt using Billable interface")
    void receipt_success() {
        Customer customer = new Customer("c1", "Alice", "0771111111", "alice@test.com");
        Car car = new Car("CAB-1234", "Toyota", "Corolla", 5);
        Invoice invoice = new Invoice("inv1", customer, car, 8.0, billingPolicy);

        when(invoiceRepository.findByIdWithTasksAndDetails("inv1")).thenReturn(Optional.of(invoice));

        String receipt = invoiceService.receipt("inv1");
        assertThat(receipt).contains("AUTOCARE SERVICE GARAGE");
        assertThat(receipt).contains("OFFICIAL RECEIPT");
        assertThat(receipt).contains("CAB-1234");
    }
}
