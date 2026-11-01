package com.autocare.domain;

import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.InvalidDiscountException;
import com.autocare.exception.InvalidStateTransitionException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Demonstrates Interface Abstraction, Static Polymorphism, and Encapsulation:
 * Implements the Billable interface to calculate payable totals and produce itemized receipts.
 * Static polymorphism is demonstrated via overloaded applyDiscount() methods.
 * <p>
 * Key Domain Invariants:
 * 1. Totals are never directly settable; they are computed dynamically from vehicle fee, tasks, discounts, and taxes.
 * 2. An invoice cannot be issued unless it has at least one task and all its tasks are COMPLETED.
 * 3. Once ISSUED, totals are frozen and no further tasks or discounts can be added.
 * 4. Once ISSUED, the invoice can transition to PAID via settle().
 */
@Entity
@Table(name = "invoices")
public class Invoice implements Billable {

    @Id
    @Column(name = "invoice_id", length = 36, nullable = false)
    private String invoiceId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "license_plate", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20, nullable = false)
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceTask> taskList = new ArrayList<>();

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "tax_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRate = BigDecimal.valueOf(8.00).setScale(2, RoundingMode.HALF_UP);

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Transient
    private Instant createdAt = Instant.now();

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "settled_at")
    private Instant settledAt;

    @Transient
    private BillingPolicy billingPolicy = BillingPolicy.defaultPolicy();

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Invoice() {
    }

    /**
     * Full parameterized constructor with custom tax rate and billing policy.
     *
     * @param invoiceId unique UUID
     * @param customer  customer responsible for payment
     * @param vehicle   serviced vehicle
     * @param taxRate   sales tax percentage (e.g. 8.0 for 8%)
     * @param policy    immutable billing policy
     */
    public Invoice(String invoiceId, Customer customer, Vehicle vehicle, double taxRate, BillingPolicy policy) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or blank");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        if (taxRate < 0) {
            throw new IllegalArgumentException("Tax rate cannot be negative");
        }
        this.invoiceId = invoiceId.trim();
        this.customer = customer;
        this.vehicle = vehicle;
        this.taxRate = BigDecimal.valueOf(taxRate).setScale(2, RoundingMode.HALF_UP);
        this.billingPolicy = policy != null ? policy : BillingPolicy.defaultPolicy();
        this.status = InvoiceStatus.OPEN;
        this.createdAt = Instant.now();
        calculateSubtotal();
        calculateTotal();
    }

    /**
     * Parameterized constructor with custom tax rate and default policy.
     *
     * @param invoiceId unique UUID
     * @param customer  customer responsible for payment
     * @param vehicle   serviced vehicle
     * @param taxRate   sales tax percentage (e.g. 8.0 for 8%)
     */
    public Invoice(String invoiceId, Customer customer, Vehicle vehicle, double taxRate) {
        this(invoiceId, customer, vehicle, taxRate, BillingPolicy.defaultPolicy());
    }

    /**
     * Parameterized constructor with custom billing policy and policy default tax rate.
     *
     * @param invoiceId unique UUID
     * @param customer  customer responsible for payment
     * @param vehicle   serviced vehicle
     * @param policy    immutable billing policy
     */
    public Invoice(String invoiceId, Customer customer, Vehicle vehicle, BillingPolicy policy) {
        this(invoiceId, customer, vehicle, policy != null ? policy.defaultTaxRate() : 8.00, policy);
    }

    /**
     * Parameterized constructor with default 8% tax rate and default policy.
     */
    public Invoice(String invoiceId, Customer customer, Vehicle vehicle) {
        this(invoiceId, customer, vehicle, 8.00, BillingPolicy.defaultPolicy());
    }

    /**
     * Convenience constructor with custom policy generating a random UUID.
     */
    public Invoice(Customer customer, Vehicle vehicle, BillingPolicy policy) {
        this(UUID.randomUUID().toString(), customer, vehicle, policy);
    }

    /**
     * Convenience constructor generating a random UUID with default 8% tax rate.
     */
    public Invoice(Customer customer, Vehicle vehicle) {
        this(UUID.randomUUID().toString(), customer, vehicle, 8.00, BillingPolicy.defaultPolicy());
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getStatus() {
        return status.name();
    }

    public InvoiceStatus getInvoiceStatus() {
        return status;
    }

    public List<ServiceTask> getTaskList() {
        return Collections.unmodifiableList(taskList);
    }

    public double getSubtotal() {
        return subtotal.doubleValue();
    }

    public double getDiscountAmount() {
        return discountAmount.doubleValue();
    }

    public double getTaxRate() {
        return taxRate.doubleValue();
    }

    public void setTaxRate(double taxRate) {
        assertOpen();
        if (taxRate < 0) {
            throw new IllegalArgumentException("Tax rate cannot be negative");
        }
        this.taxRate = BigDecimal.valueOf(taxRate).setScale(2, RoundingMode.HALF_UP);
        calculateTotal();
    }

    public double getTotalAmount() {
        return totalAmount.doubleValue();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public BillingPolicy getBillingPolicy() {
        return billingPolicy;
    }

    public void setBillingPolicy(BillingPolicy billingPolicy) {
        assertOpen();
        this.billingPolicy = billingPolicy != null ? billingPolicy : BillingPolicy.defaultPolicy();
    }

    /**
     * Adds a service task to this invoice.
     * Invariants:
     * - Invoice must be OPEN.
     * - Task must not be null or duplicate.
     *
     * @param task service task to associate
     */
    public void addTask(ServiceTask task) {
        assertOpen();
        if (task == null) {
            throw new IllegalArgumentException("Service task cannot be null");
        }
        boolean exists = taskList.stream().anyMatch(t -> t.getTaskId().equalsIgnoreCase(task.getTaskId()));
        if (exists) {
            throw new DuplicateResourceException("Task with ID " + task.getTaskId() + " is already in this invoice");
        }
        task.setInvoice(this);
        taskList.add(task);
        calculateSubtotal();
        calculateTotal();
    }

    /**
     * Demonstrates Static Polymorphism (Overload 1):
     * Applies a percentage discount to the subtotal using the invoice's billing policy.
     * Replaces any existing discount.
     * Capped at maxUnapprovedDiscountPct unless authorized with a managerId.
     *
     * @param percentage discount percentage between 0 and 100
     * @throws InvalidDiscountException if percentage <= 0 or > 100, or exceeds unapproved limit
     */
    public void applyDiscount(double percentage) {
        applyDiscount(percentage, this.billingPolicy, null);
    }

    /**
     * Applies a percentage discount with manager authorization.
     *
     * @param percentage discount percentage between 0 and 100
     * @param managerId  authorizing manager ID
     * @throws InvalidDiscountException if percentage <= 0 or > 100
     */
    public void applyDiscount(double percentage, String managerId) {
        applyDiscount(percentage, this.billingPolicy, managerId);
    }

    /**
     * Applies a percentage discount with an explicit policy and no manager.
     *
     * @param percentage discount percentage between 0 and 100
     * @param policy     billing policy containing discount caps
     */
    public void applyDiscount(double percentage, BillingPolicy policy) {
        applyDiscount(percentage, policy, null);
    }

    /**
     * Applies a percentage discount with explicit policy and authorization check.
     *
     * @param percentage discount percentage between 0 and 100
     * @param policy     billing policy containing discount caps
     * @param managerId  authorizing manager ID (required if percentage exceeds unapproved cap)
     */
    public void applyDiscount(double percentage, BillingPolicy policy, String managerId) {
        assertOpen();
        if (percentage <= 0 || percentage > 100) {
            throw new InvalidDiscountException("Percentage discount must be strictly between 0 and 100");
        }
        BillingPolicy activePolicy = policy != null ? policy : this.billingPolicy;
        if (activePolicy != null && activePolicy.requiresManagerApproval(percentage) && (managerId == null || managerId.isBlank())) {
            throw new InvalidDiscountException(
                    "Discount of " + percentage + "% exceeds the unapproved limit of " +
                            activePolicy.maxUnapprovedDiscountPct() + "%; manager approval required"
            );
        }

        BigDecimal sub = BigDecimal.valueOf(calculateSubtotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pct = BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        this.discountAmount = sub.multiply(pct).setScale(2, RoundingMode.HALF_UP);
        calculateTotal();
    }

    /**
     * Demonstrates Static Polymorphism (Overload 2):
     * Applies a fixed coupon code reduction to the invoice using current policy.
     * Replaces any existing discount.
     *
     * @param couponCode     coupon identifier
     * @param fixedReduction fixed monetary discount amount
     * @throws InvalidDiscountException if code is blank, reduction <= 0, exceeds subtotal, or exceeds coupon cap
     */
    public void applyDiscount(String couponCode, double fixedReduction) {
        applyDiscount(couponCode, fixedReduction, this.billingPolicy);
    }

    /**
     * Applies a coupon reduction validated against a specific billing policy.
     *
     * @param couponCode     coupon code
     * @param fixedReduction reduction amount
     * @param policy         billing policy holding coupon registry
     */
    public void applyDiscount(String couponCode, double fixedReduction, BillingPolicy policy) {
        assertOpen();
        if (couponCode == null || couponCode.isBlank()) {
            throw new InvalidDiscountException("Coupon code cannot be null or blank");
        }
        if (fixedReduction <= 0) {
            throw new InvalidDiscountException("Coupon reduction must be strictly positive");
        }

        BigDecimal sub = BigDecimal.valueOf(calculateSubtotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal reduction = BigDecimal.valueOf(fixedReduction).setScale(2, RoundingMode.HALF_UP);

        if (reduction.compareTo(sub) > 0) {
            throw new InvalidDiscountException("Coupon reduction cannot exceed invoice subtotal");
        }

        BillingPolicy activePolicy = policy != null ? policy : this.billingPolicy;
        if (activePolicy != null) {
            if (!activePolicy.hasCoupon(couponCode)) {
                throw new InvalidDiscountException("Unknown or invalid coupon code: " + couponCode);
            }
            double maxAllowed = activePolicy.getMaxCouponReduction(couponCode);
            if (fixedReduction > maxAllowed) {
                throw new InvalidDiscountException(
                        "Discount reduction " + fixedReduction + " exceeds coupon maximum of " + maxAllowed
                );
            }
        }

        this.discountAmount = reduction;
        calculateTotal();
    }

    /**
     * Demonstrates Dynamic Polymorphism:
     * Calculates subtotal: vehicle.getBaseServiceFee() + Σ task.calculateTaskCost().
     * Dynamically dispatches getBaseServiceFee() on polymorphic Vehicle (Car, Van, Motorcycle).
     * Once ISSUED, totals are frozen and this method returns the frozen subtotal.
     *
     * @return invoice subtotal
     */
    public double calculateSubtotal() {
        if (this.status != InvoiceStatus.OPEN) {
            return this.subtotal.doubleValue();
        }
        BigDecimal baseFee = BigDecimal.valueOf(vehicle != null ? vehicle.getBaseServiceFee() : 0.0)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taskTotal = BigDecimal.ZERO;
        for (ServiceTask task : taskList) {
            taskTotal = taskTotal.add(BigDecimal.valueOf(task.calculateTaskCost()));
        }

        this.subtotal = baseFee.add(taskTotal).setScale(2, RoundingMode.HALF_UP);
        return this.subtotal.doubleValue();
    }

    /**
     * Demonstrates Interface Realization (Billable):
     * Calculates final payable total: (subtotal - discountAmount) + tax.
     * Tax formula: (subtotal - discountAmount) * taxRate / 100.
     * Once ISSUED, totals are frozen and this method returns the frozen totalAmount.
     *
     * @return net payable total amount
     */
    @Override
    public double calculateTotal() {
        if (this.status != InvoiceStatus.OPEN) {
            return this.totalAmount.doubleValue();
        }
        BigDecimal sub = BigDecimal.valueOf(calculateSubtotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal disc = this.discountAmount.min(sub);
        BigDecimal taxable = sub.subtract(disc).setScale(2, RoundingMode.HALF_UP);

        BigDecimal tax = taxable.multiply(this.taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        this.totalAmount = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);
        return this.totalAmount.doubleValue();
    }

    /**
     * Issues the invoice, locking totals and preventing any further changes.
     * Invariants:
     * - Must currently be in OPEN status.
     * - Must have at least one service task.
     * - Every service task in the invoice must be in COMPLETED status.
     */
    public void issue() {
        assertOpen();
        if (taskList.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot issue an invoice with no service tasks");
        }
        boolean allCompleted = taskList.stream()
                .allMatch(t -> t.getTaskStatus() == TaskStatus.COMPLETED);
        if (!allCompleted) {
            throw new BusinessRuleViolationException("All service tasks must be COMPLETED before issuing invoice");
        }

        calculateSubtotal();
        calculateTotal();
        this.status = InvoiceStatus.ISSUED;
        this.issuedAt = Instant.now();
    }

    /**
     * Settles the invoice once paid.
     * Invariant: invoice must be in ISSUED status.
     */
    public void settle() {
        if (this.status != InvoiceStatus.ISSUED) {
            throw new InvalidStateTransitionException(
                    "Cannot settle invoice in status " + this.status + "; invoice must be ISSUED"
            );
        }
        this.status = InvoiceStatus.PAID;
        this.settledAt = Instant.now();
    }

    /**
     * Demonstrates Interface Realization (Billable):
     * Formats an itemized printable receipt displaying base fees, line items, discounts, taxes, and total.
     *
     * @return formatted receipt string
     */
    @Override
    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================\n");
        sb.append("                      AUTOCARE SERVICE GARAGE                   \n");
        sb.append("                          OFFICIAL RECEIPT                      \n");
        sb.append("================================================================\n");
        sb.append(String.format(Locale.US, "Invoice ID:   %s\n", invoiceId));
        sb.append(String.format(Locale.US, "Status:       %s\n", status));
        sb.append(String.format(Locale.US, "Customer:     %s (%s)\n", customer.getName(), customer.getContactDetails()));
        sb.append(String.format(Locale.US, "Vehicle:      %s - %s %s (%s)\n",
                vehicle.getLicensePlate(), vehicle.getBrand(), vehicle.getModel(), vehicle.getVehicleType()));
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format(Locale.US, "%-45s %15.2f\n", "Vehicle Base Service Fee (" + vehicle.getVehicleType() + ")", vehicle.getBaseServiceFee()));

        for (int i = 0; i < taskList.size(); i++) {
            ServiceTask t = taskList.get(i);
            sb.append(String.format(Locale.US, "\nTask %d: %s (Status: %s)\n", (i + 1), t.getDescription(), t.getStatus()));
            if (t.getBillableHours() > 0) {
                sb.append(String.format(Locale.US, "  - Labor (%.2f hrs): %38.2f\n", t.getBillableHours(), t.getLaborCharge()));
            }
            for (TaskUsedPart p : t.getUsedParts()) {
                sb.append(String.format(Locale.US, "  - Part: %s x %d @ %.2f: %27.2f\n",
                        p.getItem().getPartName(), p.getQuantity(), p.getUnitPrice(), p.getSubtotal()));
            }
            sb.append(String.format(Locale.US, "  Task Total: %47.2f\n", t.getTotalTaskCost()));
        }

        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format(Locale.US, "%-45s %15.2f\n", "Subtotal:", getSubtotal()));
        if (getDiscountAmount() > 0) {
            sb.append(String.format(Locale.US, "%-45s %15.2f\n", "Discount:", -getDiscountAmount()));
        }
        sb.append(String.format(Locale.US, "%-45s %15.2f\n", String.format(Locale.US, "Tax (%.2f%%):", getTaxRate()), calculateTaxAmount()));
        sb.append("================================================================\n");
        sb.append(String.format(Locale.US, "%-45s %15.2f\n", "TOTAL DUE:", getTotalAmount()));
        sb.append("================================================================\n");
        return sb.toString();
    }

    private double calculateTaxAmount() {
        BigDecimal sub = BigDecimal.valueOf(getSubtotal());
        BigDecimal disc = this.discountAmount.min(sub);
        BigDecimal taxable = sub.subtract(disc);
        return taxable.multiply(this.taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void assertOpen() {
        if (this.status != InvoiceStatus.OPEN) {
            throw new BusinessRuleViolationException(
                    "Cannot modify invoice in status " + this.status + "; invoice must be OPEN"
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice invoice)) return false;
        return Objects.equals(invoiceId, invoice.invoiceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceId);
    }
}
