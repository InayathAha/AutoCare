package com.autocare.controller;

import com.autocare.dto.*;
import com.autocare.exception.*;
import com.autocare.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceController Web Layer Tests")
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        InvoiceController controller = new InvoiceController(invoiceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/invoices - 201 Created on valid input")
    void openInvoice_success() throws Exception {
        OpenInvoiceRequest req = new OpenInvoiceRequest("c1", "CAB-1234");
        InvoiceResponse res = new InvoiceResponse("inv1", "c1", "Alice", "CAB-1234", "CAR", 50.0, Collections.emptyList(), 50.0, 0, 8.0, 4.0, 54.0, "OPEN", "2026-11-02T00:00:00Z", null);

        when(invoiceService.open(any(OpenInvoiceRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.invoiceId", is("inv1")))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices - 422 BUSINESS_RULE_VIOLATION when vehicle not owned by customer")
    void openInvoice_wrongOwner() throws Exception {
        OpenInvoiceRequest req = new OpenInvoiceRequest("c1", "CAB-1234");
        when(invoiceService.open(any(OpenInvoiceRequest.class)))
                .thenThrow(new BusinessRuleViolationException("Vehicle CAB-1234 is not registered to customer c1"));

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("GET /api/v1/invoices - 200 OK list with query param")
    void listInvoices_success() throws Exception {
        InvoiceSummary summary = new InvoiceSummary("inv1", "Alice", "CAB-1234", "OPEN", 54.0, "2026-11-02T00:00:00Z");
        when(invoiceService.list("OPEN")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/invoices").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceId", is("inv1")));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id} - 200 OK when found, 404 when missing")
    void getInvoice_endpoints() throws Exception {
        InvoiceResponse res = new InvoiceResponse("inv1", "c1", "Alice", "CAB-1234", "CAR", 50.0, Collections.emptyList(), 50.0, 0, 8.0, 4.0, 54.0, "OPEN", "2026-11-02T00:00:00Z", null);
        when(invoiceService.get("inv1")).thenReturn(res);
        when(invoiceService.get("unknown")).thenThrow(new ResourceNotFoundException("Invoice not found: unknown"));

        mockMvc.perform(get("/api/v1/invoices/inv1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId", is("inv1")));

        mockMvc.perform(get("/api/v1/invoices/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/discount/percentage - 200 OK on success, 422 INVALID_DISCOUNT when unapproved cap exceeded")
    void applyPercentageDiscount_endpoints() throws Exception {
        DiscountPercentageRequest validReq = new DiscountPercentageRequest(10.0, null);
        InvoiceResponse res = new InvoiceResponse("inv1", "c1", "Alice", "CAB-1234", "CAR", 50.0, Collections.emptyList(), 50.0, 5.0, 8.0, 3.6, 48.6, "OPEN", "2026-11-02T00:00:00Z", null);
        when(invoiceService.applyPercentageDiscount(eq("inv1"), any(DiscountPercentageRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/invoices/inv1/discount/percentage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount", is(5.0)));

        when(invoiceService.applyPercentageDiscount(eq("inv1"), any(DiscountPercentageRequest.class)))
                .thenThrow(new InvalidDiscountException("Discount of 15% exceeds the unapproved limit of 10%; manager approval required"));

        mockMvc.perform(post("/api/v1/invoices/inv1/discount/percentage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DiscountPercentageRequest(15.0, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_DISCOUNT")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/discount/coupon - 200 OK on success, 422 INVALID_DISCOUNT on bad coupon")
    void applyCouponDiscount_endpoints() throws Exception {
        DiscountCouponRequest validReq = new DiscountCouponRequest("SAVE10", 10.0);
        InvoiceResponse res = new InvoiceResponse("inv1", "c1", "Alice", "CAB-1234", "CAR", 50.0, Collections.emptyList(), 50.0, 10.0, 8.0, 3.2, 43.2, "OPEN", "2026-11-02T00:00:00Z", null);
        when(invoiceService.applyCouponDiscount(eq("inv1"), any(DiscountCouponRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/invoices/inv1/discount/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount", is(10.0)));

        when(invoiceService.applyCouponDiscount(eq("inv1"), any(DiscountCouponRequest.class)))
                .thenThrow(new InvalidDiscountException("Unknown or invalid coupon code: BOGUS"));

        mockMvc.perform(post("/api/v1/invoices/inv1/discount/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DiscountCouponRequest("BOGUS", 10.0))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_DISCOUNT")));
    }

    @Test
    @DisplayName("PUT /api/v1/invoices/{id}/tax - 200 OK on success, 400 on negative tax")
    void setTaxRate_endpoints() throws Exception {
        TaxRateRequest validReq = new TaxRateRequest(12.0);
        InvoiceResponse res = new InvoiceResponse("inv1", "c1", "Alice", "CAB-1234", "CAR", 50.0, Collections.emptyList(), 50.0, 0, 12.0, 6.0, 56.0, "OPEN", "2026-11-02T00:00:00Z", null);
        when(invoiceService.setTax(eq("inv1"), any(TaxRateRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/v1/invoices/inv1/tax")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxRate", is(12.0)));

        TaxRateRequest invalidReq = new TaxRateRequest(-5.0);
        mockMvc.perform(put("/api/v1/invoices/inv1/tax")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/issue - 200 OK on success, 422 on incomplete tasks")
    void issueInvoice_endpoints() throws Exception {
        InvoiceResponse res = new InvoiceResponse("inv1", "c1", "Alice", "CAB-1234", "CAR", 50.0, Collections.emptyList(), 50.0, 0, 8.0, 4.0, 54.0, "ISSUED", "2026-11-02T00:00:00Z", null);
        when(invoiceService.issue("inv1")).thenReturn(res);

        mockMvc.perform(post("/api/v1/invoices/inv1/issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ISSUED")));

        when(invoiceService.issue("inv2"))
                .thenThrow(new BusinessRuleViolationException("All service tasks must be COMPLETED before issuing invoice"));

        mockMvc.perform(post("/api/v1/invoices/inv2/issue"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{id}/settle - 200 OK on success, 409 INVALID_STATE_TRANSITION if not ISSUED")
    void settleInvoice_endpoints() throws Exception {
        InvoiceResponse res = new InvoiceResponse("inv1", "c1", "Alice", "CAB-1234", "CAR", 50.0, Collections.emptyList(), 50.0, 0, 8.0, 4.0, 54.0, "PAID", "2026-11-02T00:00:00Z", "2026-11-02T01:00:00Z");
        when(invoiceService.settle("inv1")).thenReturn(res);

        mockMvc.perform(post("/api/v1/invoices/inv1/settle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));

        when(invoiceService.settle("inv2"))
                .thenThrow(new InvalidStateTransitionException("Cannot settle invoice in status OPEN; invoice must be ISSUED"));

        mockMvc.perform(post("/api/v1/invoices/inv2/settle"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_STATE_TRANSITION")));
    }

    @Test
    @DisplayName("GET /api/v1/invoices/{id}/receipt - 200 OK with text/plain, 404 when missing")
    void getReceipt_endpoints() throws Exception {
        when(invoiceService.receipt("inv1")).thenReturn("OFFICIAL RECEIPT\nCAB-1234");

        mockMvc.perform(get("/api/v1/invoices/inv1/receipt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OFFICIAL RECEIPT")));

        when(invoiceService.receipt("unknown")).thenThrow(new ResourceNotFoundException("Invoice not found: unknown"));

        mockMvc.perform(get("/api/v1/invoices/unknown/receipt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }
}
