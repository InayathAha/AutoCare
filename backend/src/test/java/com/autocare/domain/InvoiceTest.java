package com.autocare.domain;

import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.InvalidDiscountException;
import com.autocare.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Invoice & Billable - Worked Example, Discounts & State Invariants")
class InvoiceTest {

    private Customer customer;
    private Car car;
    private Mechanic mechanic;
    private InventoryItem oilFilter;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        customer = new Customer("c-10", "Arthur Dent", "0712345678", "arthur@galaxy.com");
        car = new Car("WP-CAR-42", "Ford", "Prefect", customer, 5);
        mechanic = new Mechanic("m-10", "Ford Prefect", "0711122233", "ford@galaxy.com", 1000.0, "Engine", 20.0);
        oilFilter = new InventoryItem("FLT-OIL", "Oil Filter", 8.00, 20, 5);
        invoice = new Invoice("inv-42", customer, car, 8.00);
    }

    @Test
    @DisplayName("Worked Example (02-domain-model.md §10): Car 50 + Task 46 - 10% disc + 8% tax = 93.31")
    void workedExampleFixtureYields93Point31() {
        // Car base fee = 50.00
        assertThat(car.getBaseServiceFee()).isEqualTo(50.00);

        // One task with 2 x oil filter at 8.00 (= 16.00) and 1.5 billable hours at 20.00/h (= 30.00)
        ServiceTask task = new ServiceTask("t-oil", "Oil change service", mechanic);
        task.addUsedPart(oilFilter, 2);
        task.recordBillableHours(1.5, 20.00);

        assertThat(task.getPartsCost()).isEqualTo(16.00);
        assertThat(task.getLaborCharge()).isEqualTo(30.00);
        assertThat(task.calculateTaskCost()).isEqualTo(46.00);

        // Attach task to invoice
        invoice.addTask(task);

        // Subtotal = 50.00 + 46.00 = 96.00
        assertThat(invoice.calculateSubtotal()).isEqualTo(96.00);
        assertThat(invoice.getSubtotal()).isEqualTo(96.00);

        // Apply 10% discount -> 96.00 * 10% = 9.60
        invoice.applyDiscount(10.0);
        assertThat(invoice.getDiscountAmount()).isEqualTo(9.60);

        // Taxable = 96.00 - 9.60 = 86.40
        // Tax 8% = 86.40 * 0.08 = 6.912 -> rounded to 6.91
        // Total = 86.40 + 6.91 = 93.31
        double total = invoice.calculateTotal();
        assertThat(total).isEqualTo(93.31);
        assertThat(invoice.getTotalAmount()).isEqualTo(93.31);
    }

    @Test
    @DisplayName("Overload 1: applyDiscount(percentage) rejects non-positive and > 100 percentages")
    void applyDiscountPercentageInvalidInputs() {
        assertThatThrownBy(() -> invoice.applyDiscount(0.0))
                .isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> invoice.applyDiscount(-5.0))
                .isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> invoice.applyDiscount(100.1))
                .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    @DisplayName("Overload 1: Direct applyDiscount(percentage) above policy cap throws without manager approval")
    void applyDiscountPercentageDirectAboveCapThrows() {
        ServiceTask task = new ServiceTask("t-cap", "Brake service", mechanic);
        task.recordBillableHours(1.0, 20.00);
        invoice.addTask(task);

        // Default policy cap is 10%; 15% directly without manager throws
        assertThatThrownBy(() -> invoice.applyDiscount(15.0))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("manager approval required");
    }

    @Test
    @DisplayName("Overload 1: Percentage discount above policy cap requires manager approval")
    void applyDiscountPercentageAboveCapRequiresManager() {
        BillingPolicy policy = BillingPolicy.defaultPolicy(); // cap 10%
        ServiceTask task = new ServiceTask("t-1", "Tune up", mechanic);
        task.recordBillableHours(2.5, 20.00); // 50.00
        invoice.addTask(task); // Subtotal = 50 + 50 = 100.00

        // 15% without manager id throws InvalidDiscountException
        assertThatThrownBy(() -> invoice.applyDiscount(15.0, policy, null))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("manager approval required");

        // 15% with manager id succeeds
        invoice.applyDiscount(15.0, policy, "MGR-007");
        assertThat(invoice.getDiscountAmount()).isEqualTo(15.00);
    }

    @Test
    @DisplayName("Overload 2: applyDiscount(couponCode, fixedReduction) applies valid coupon reduction")
    void applyDiscountCouponSuccess() {
        ServiceTask task = new ServiceTask("t-2", "General checkup", mechanic);
        task.recordBillableHours(1.0, 20.00);
        invoice.addTask(task); // Subtotal = 50 + 20 = 70.00

        BillingPolicy policy = BillingPolicy.defaultPolicy(); // SAVE10 allows up to 10.00
        invoice.applyDiscount("SAVE10", 10.00, policy);

        assertThat(invoice.getDiscountAmount()).isEqualTo(10.00);
    }

    @Test
    @DisplayName("Overload 2: applyDiscount with coupon rejects unknown coupon, excess reduction, and non-positive")
    void applyDiscountCouponInvalidInputs() {
        BillingPolicy policy = BillingPolicy.defaultPolicy();

        // Unknown coupon
        assertThatThrownBy(() -> invoice.applyDiscount("UNKNOWN99", 5.00, policy))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("Unknown");

        // Reduction exceeds coupon maximum
        assertThatThrownBy(() -> invoice.applyDiscount("SAVE10", 15.00, policy))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("exceeds coupon maximum");

        // Non-positive reduction
        assertThatThrownBy(() -> invoice.applyDiscount("SAVE10", 0.00, policy))
                .isInstanceOf(InvalidDiscountException.class);

        // Blank coupon code
        assertThatThrownBy(() -> invoice.applyDiscount("   ", 5.00, policy))
                .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    @DisplayName("Overload 2: applyDiscount coupon reduction cannot exceed subtotal")
    void applyDiscountCouponCannotExceedSubtotal() {
        BillingPolicy policy = new BillingPolicy(20.0, 20.0, 8.0, java.util.Map.of("BIG100", 100.0));
        // Subtotal = car base fee 50.00
        assertThatThrownBy(() -> invoice.applyDiscount("BIG100", 60.00, policy))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("cannot exceed invoice subtotal");
    }

    @Test
    @DisplayName("issue() succeeds when all tasks are COMPLETED and freezes totals")
    void issueSuccessWithCompletedTasks() {
        ServiceTask task1 = new ServiceTask("t-01", "Brake inspection", mechanic);
        task1.startTask();
        task1.markCompleted();

        ServiceTask task2 = new ServiceTask("t-02", "Fluid top up", mechanic);
        task2.startTask();
        task2.markCompleted();

        invoice.addTask(task1);
        invoice.addTask(task2);

        double subtotalBefore = invoice.calculateSubtotal();
        double totalBefore = invoice.calculateTotal();

        invoice.issue();

        assertThat(invoice.getStatus()).isEqualTo("ISSUED");
        assertThat(invoice.getIssuedAt()).isNotNull();
        // Verify totals remain frozen
        assertThat(invoice.getSubtotal()).isEqualTo(subtotalBefore);
        assertThat(invoice.getTotalAmount()).isEqualTo(totalBefore);
        assertThat(invoice.calculateSubtotal()).isEqualTo(subtotalBefore);
        assertThat(invoice.calculateTotal()).isEqualTo(totalBefore);
    }

    @Test
    @DisplayName("issue() rejects when invoice has no tasks")
    void issueRejectsEmptyTasks() {
        assertThatThrownBy(() -> invoice.issue())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no service tasks");
    }

    @Test
    @DisplayName("issue() rejects when any task is still PENDING or IN_PROGRESS")
    void issueRejectsIncompleteTasks() {
        ServiceTask task1 = new ServiceTask("t-01", "Brake inspection", mechanic);
        task1.startTask();
        task1.markCompleted();

        ServiceTask task2 = new ServiceTask("t-02", "Fluid top up", mechanic);
        task2.startTask(); // IN_PROGRESS, not COMPLETED!

        invoice.addTask(task1);
        invoice.addTask(task2);

        assertThatThrownBy(() -> invoice.issue())
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("COMPLETED before issuing");
    }

    @Test
    @DisplayName("addTask and discount mutations throw BusinessRuleViolationException after issue")
    void mutationsRejectedAfterIssue() {
        ServiceTask task = new ServiceTask("t-01", "Service", mechanic);
        task.startTask();
        task.markCompleted();

        invoice.addTask(task);
        invoice.issue();

        ServiceTask lateTask = new ServiceTask("t-02", "Late task", mechanic);
        assertThatThrownBy(() -> invoice.addTask(lateTask))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("must be OPEN");

        assertThatThrownBy(() -> invoice.applyDiscount(5.0))
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThatThrownBy(() -> invoice.setTaxRate(10.0))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("settle() transitions invoice from ISSUED to PAID and sets settledAt")
    void settleSuccess() {
        ServiceTask task = new ServiceTask("t-01", "Service", mechanic);
        task.startTask();
        task.markCompleted();
        invoice.addTask(task);
        invoice.issue();

        invoice.settle();

        assertThat(invoice.getStatus()).isEqualTo("PAID");
        assertThat(invoice.getSettledAt()).isNotNull();
    }

    @Test
    @DisplayName("settle() throws InvalidStateTransitionException if invoice is OPEN")
    void settleRejectsOpenInvoice() {
        assertThatThrownBy(() -> invoice.settle())
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("must be ISSUED");
    }

    @Test
    @DisplayName("generateReceipt contains itemized breakdown: base fee, tasks, parts, tax, and total")
    void generateReceiptContent() {
        ServiceTask task = new ServiceTask("t-rec", "Full service", mechanic);
        task.addUsedPart(oilFilter, 2);
        task.recordBillableHours(1.5, 20.00);
        invoice.addTask(task);
        invoice.applyDiscount(10.0);

        String receipt = invoice.generateReceipt();

        assertThat(receipt)
                .contains("OFFICIAL RECEIPT")
                .contains("inv-42")
                .contains("Arthur Dent")
                .contains("WP-CAR-42")
                .contains("Vehicle Base Service Fee (Car)")
                .contains("50.00")
                .contains("Full service")
                .contains("Oil Filter")
                .contains("Subtotal:")
                .contains("Discount:")
                .contains("Tax (8.00%):")
                .contains("TOTAL DUE:")
                .contains("93.31");
    }

    @Test
    @DisplayName("Polymorphic interface usage: Invoice operates seamlessly as a Billable in List<Billable>")
    void invoiceUsableAsBillable() {
        ServiceTask task = new ServiceTask("t-b", "Polymorphic task", mechanic);
        task.recordBillableHours(1.0, 20.0);
        invoice.addTask(task);

        Billable billable = invoice;
        List<Billable> billables = List.of(billable);

        assertThat(billables.get(0).calculateTotal()).isGreaterThan(0.0);
        assertThat(billables.get(0).generateReceipt()).contains("OFFICIAL RECEIPT");
    }

    @Test
    @DisplayName("BillingPolicy: Configurable coupons, caps, and labor rate keep domain Spring-free")
    void billingPolicyConfigurableValuesIntegration() {
        // Create custom policy: cap 25%, labor 35.00/h, tax 10%, coupons VIP30 (30.00)
        BillingPolicy customPolicy = new BillingPolicy(
                25.0,
                35.00,
                10.0,
                java.util.Map.of("VIP30", 30.00)
        );

        // Construct invoice with custom policy
        Invoice customInvoice = new Invoice("inv-custom", customer, car, customPolicy);
        assertThat(customInvoice.getBillingPolicy().laborRatePerHour()).isEqualTo(35.00);
        assertThat(customInvoice.getTaxRate()).isEqualTo(10.00);

        // Task uses policy labor rate
        ServiceTask task = new ServiceTask("t-custom", "Custom rate task", mechanic);
        task.recordBillableHours(2.0, customPolicy); // 2.0 * 35.00 = 70.00
        assertThat(task.getLaborCharge()).isEqualTo(70.00);

        customInvoice.addTask(task); // Subtotal = 50.00 + 70.00 = 120.00
        assertThat(customInvoice.calculateSubtotal()).isEqualTo(120.00);

        // 20% discount is below the custom 25% cap, so it succeeds without manager approval
        customInvoice.applyDiscount(20.0);
        assertThat(customInvoice.getDiscountAmount()).isEqualTo(24.00); // 120 * 0.20

        // Fixed coupon discount from custom policy
        customInvoice.applyDiscount("VIP30", 30.00);
        assertThat(customInvoice.getDiscountAmount()).isEqualTo(30.00);

        // Subtotal 120 - discount 30 = 90. Tax 10% = 9. Total = 99.00
        assertThat(customInvoice.calculateTotal()).isEqualTo(99.00);
    }
}
