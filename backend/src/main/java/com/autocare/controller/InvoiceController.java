package com.autocare.controller;

import com.autocare.dto.*;
import com.autocare.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing billing, invoices, polymorphic discounts, taxes, and settlement.
 * Base path: /api/v1/invoices
 */
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> openInvoice(@Valid @RequestBody OpenInvoiceRequest req) {
        InvoiceResponse response = invoiceService.open(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.invoiceId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceSummary>> listInvoices(@RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(invoiceService.list(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable("id") String id) {
        return ResponseEntity.ok(invoiceService.get(id));
    }

    @PostMapping("/{id}/discount/percentage")
    public ResponseEntity<InvoiceResponse> applyPercentageDiscount(@PathVariable("id") String id,
                                                                   @Valid @RequestBody DiscountPercentageRequest req) {
        return ResponseEntity.ok(invoiceService.applyPercentageDiscount(id, req));
    }

    @PostMapping("/{id}/discount/coupon")
    public ResponseEntity<InvoiceResponse> applyCouponDiscount(@PathVariable("id") String id,
                                                               @Valid @RequestBody DiscountCouponRequest req) {
        return ResponseEntity.ok(invoiceService.applyCouponDiscount(id, req));
    }

    @PutMapping("/{id}/tax")
    public ResponseEntity<InvoiceResponse> setTaxRate(@PathVariable("id") String id,
                                                      @Valid @RequestBody TaxRateRequest req) {
        return ResponseEntity.ok(invoiceService.setTax(id, req));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<InvoiceResponse> issueInvoice(@PathVariable("id") String id) {
        return ResponseEntity.ok(invoiceService.issue(id));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<InvoiceResponse> settleInvoice(@PathVariable("id") String id) {
        return ResponseEntity.ok(invoiceService.settle(id));
    }

    @GetMapping(value = "/{id}/receipt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getReceipt(@PathVariable("id") String id) {
        return ResponseEntity.ok(invoiceService.receipt(id));
    }
}
