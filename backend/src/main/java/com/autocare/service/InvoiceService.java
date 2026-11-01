package com.autocare.service;

import com.autocare.config.BillingProperties;
import com.autocare.domain.*;
import com.autocare.dto.*;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.mapper.InvoiceMapper;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.EmployeeRepository;
import com.autocare.repository.InvoiceRepository;
import com.autocare.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating billing, static polymorphism discounts, tax settings, and invoice lifecycle.
 * Invariants: Total calculation is never set directly; frozen upon issuing; settlement awards loyalty points.
 */
@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final InvoiceMapper invoiceMapper;
    private final BillingPolicy billingPolicy;
    private final BillingProperties billingProperties;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          CustomerRepository customerRepository,
                          VehicleRepository vehicleRepository,
                          EmployeeRepository employeeRepository,
                          InvoiceMapper invoiceMapper,
                          BillingPolicy billingPolicy,
                          BillingProperties billingProperties) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
        this.invoiceMapper = invoiceMapper;
        this.billingPolicy = billingPolicy;
        this.billingProperties = billingProperties;
    }

    public InvoiceResponse open(OpenInvoiceRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.customerId()));

        String plate = req.licensePlate().trim().toUpperCase();
        Vehicle vehicle = vehicleRepository.findById(plate)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + plate));

        if (vehicle.getOwner() == null || !vehicle.getOwner().getId().equals(customer.getId())) {
            throw new BusinessRuleViolationException(
                    "Vehicle " + plate + " is not registered to customer " + customer.getId()
            );
        }

        Invoice invoice = new Invoice(
                UUID.randomUUID().toString(),
                customer,
                vehicle,
                billingPolicy.defaultTaxRate(),
                billingPolicy
        );

        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(String invoiceId) {
        return invoiceMapper.toResponse(findInvoiceWithDetailsOrThrow(invoiceId));
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummary> list(String statusStr) {
        List<Invoice> invoices;
        if (statusStr != null && !statusStr.isBlank()) {
            InvoiceStatus status;
            try {
                status = InvoiceStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleViolationException("Unknown invoice status: " + statusStr);
            }
            invoices = invoiceRepository.findByStatus(status);
        } else {
            invoices = invoiceRepository.findAll();
        }
        return invoices.stream().map(invoiceMapper::toSummary).toList();
    }

    public InvoiceResponse applyPercentageDiscount(String invoiceId, DiscountPercentageRequest req) {
        Invoice invoice = findInvoiceWithDetailsOrThrow(invoiceId);
        invoice.setBillingPolicy(billingPolicy);

        if (req.managerId() != null && !req.managerId().isBlank()) {
            Employee employee = employeeRepository.findById(req.managerId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + req.managerId()));

            if (!(employee instanceof Manager manager)) {
                throw new BusinessRuleViolationException("Employee " + req.managerId() + " is not a Manager");
            }

            manager.approveDiscount(invoice, req.percentage());
        } else {
            invoice.applyDiscount(req.percentage());
        }

        return invoiceMapper.toResponse(invoice);
    }

    public InvoiceResponse applyCouponDiscount(String invoiceId, DiscountCouponRequest req) {
        Invoice invoice = findInvoiceWithDetailsOrThrow(invoiceId);
        invoice.setBillingPolicy(billingPolicy);
        invoice.applyDiscount(req.couponCode(), req.fixedReduction());
        return invoiceMapper.toResponse(invoice);
    }

    public InvoiceResponse setTax(String invoiceId, TaxRateRequest req) {
        Invoice invoice = findInvoiceWithDetailsOrThrow(invoiceId);
        invoice.setTaxRate(req.taxRate());
        return invoiceMapper.toResponse(invoice);
    }

    public InvoiceResponse issue(String invoiceId) {
        Invoice invoice = findInvoiceWithDetailsOrThrow(invoiceId);
        invoice.issue(); // Invariant: must be OPEN, at least one task, all tasks COMPLETED
        return invoiceMapper.toResponse(invoice);
    }

    public InvoiceResponse settle(String invoiceId) {
        Invoice invoice = findInvoiceWithDetailsOrThrow(invoiceId);
        invoice.settle(); // Invariant: must be ISSUED -> transitions to PAID

        double pointsRate = (billingProperties != null && billingProperties.getLoyalty() != null)
                ? billingProperties.getLoyalty().getPointsPerCurrencyUnit()
                : 0.1;
        int points = (int) Math.floor(invoice.getTotalAmount() * pointsRate);
        if (points > 0) {
            invoice.getCustomer().addLoyaltyPoints(points);
        }

        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public String receipt(String invoiceId) {
        Invoice invoice = findInvoiceWithDetailsOrThrow(invoiceId);
        return invoice.generateReceipt(); // Interface realization: Billable.generateReceipt()
    }

    private Invoice findInvoiceWithDetailsOrThrow(String invoiceId) {
        return invoiceRepository.findByIdWithTasksAndDetails(invoiceId)
                .or(() -> invoiceRepository.findById(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
    }
}
