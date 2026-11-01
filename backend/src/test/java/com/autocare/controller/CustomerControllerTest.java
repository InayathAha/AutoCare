package com.autocare.controller;

import com.autocare.dto.*;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.GlobalExceptionHandler;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerController Web Layer Tests")
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CustomerController controller = new CustomerController(customerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers - 201 Created on valid input")
    void createCustomer_success() throws Exception {
        CustomerRequest req = new CustomerRequest("Alice Smith", "0771234567", "alice@test.com");
        CustomerResponse res = new CustomerResponse("c1", "Alice Smith", "0771234567", "alice@test.com", 0, 0, "2026-11-02T00:00:00Z");

        when(customerService.create(any(CustomerRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", is("c1")))
                .andExpect(jsonPath("$.name", is("Alice Smith")));
    }

    @Test
    @DisplayName("POST /api/v1/customers - 400 VALIDATION_FAILED on invalid input")
    void createCustomer_validationFailed() throws Exception {
        CustomerRequest req = new CustomerRequest("", "", "invalid-email");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/customers - 409 DUPLICATE_RESOURCE on taken email")
    void createCustomer_duplicate() throws Exception {
        CustomerRequest req = new CustomerRequest("Alice Smith", "0771234567", "alice@test.com");
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("A customer with email alice@test.com already exists"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("alice@test.com")));
    }

    @Test
    @DisplayName("GET /api/v1/customers - 200 OK list")
    void listCustomers_success() throws Exception {
        CustomerResponse res = new CustomerResponse("c1", "Alice Smith", "0771234567", "alice@test.com", 0, 0, null);
        when(customerService.list("Alice")).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/customers").param("q", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Alice Smith")));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - 200 OK when found")
    void getCustomer_success() throws Exception {
        CustomerResponse res = new CustomerResponse("c1", "Alice", "0771", "alice@test.com", 0, 0, null);
        when(customerService.get("c1")).thenReturn(res);

        mockMvc.perform(get("/api/v1/customers/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("c1")));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - 404 NOT_FOUND when missing")
    void getCustomer_notFound() throws Exception {
        when(customerService.get("unknown")).thenThrow(new ResourceNotFoundException("Customer not found: unknown"));

        mockMvc.perform(get("/api/v1/customers/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} - 200 OK on update")
    void updateCustomer_success() throws Exception {
        CustomerRequest req = new CustomerRequest("Alice New", "0772222222", "alice.new@test.com");
        CustomerResponse res = new CustomerResponse("c1", "Alice New", "0772222222", "alice.new@test.com", 0, 0, null);
        when(customerService.update(eq("c1"), any(CustomerRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/v1/customers/c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice New")));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - 204 No Content")
    void deleteCustomer_success() throws Exception {
        doNothing().when(customerService).delete("c1");

        mockMvc.perform(delete("/api/v1/customers/c1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} - 409 FK_CONSTRAINT when has vehicles")
    void deleteCustomer_hasVehicles_throws() throws Exception {
        doThrow(new DataIntegrityViolationException("Cannot delete customer with registered vehicles"))
                .when(customerService).delete("c1");

        mockMvc.perform(delete("/api/v1/customers/c1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("FK_CONSTRAINT")));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{id}/vehicles - 201 Created")
    void registerVehicle_success() throws Exception {
        VehicleRequest req = new VehicleRequest("CAR", "CAB-1234", "Toyota", "Corolla", 5, null, null);
        VehicleResponse res = new VehicleResponse("CAB-1234", "CAR", "Toyota", "Corolla", 50.0, "c1", 5, null, null);
        when(customerService.registerVehicle(eq("c1"), any(VehicleRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/customers/c1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate", is("CAB-1234")));
    }

    @Test
    @DisplayName("GET /api/v1/vehicles/{licensePlate} - 200 OK")
    void getVehicle_success() throws Exception {
        VehicleResponse res = new VehicleResponse("CAB-1234", "CAR", "Toyota", "Corolla", 50.0, "c1", 5, null, null);
        when(customerService.getVehicle("CAB-1234")).thenReturn(res);

        mockMvc.perform(get("/api/v1/vehicles/CAB-1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate", is("CAB-1234")));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id}/service-history - 200 OK")
    void getServiceHistory_success() throws Exception {
        InvoiceSummary summary = new InvoiceSummary("inv1", "Alice", "CAB-1234", "ISSUED", 93.31, "2026-11-02T00:00:00Z");
        when(customerService.serviceHistory("c1")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/customers/c1/service-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceId", is("inv1")));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{id}/loyalty/redeem - 200 OK on success and 422 on failure")
    void redeemLoyalty_endpoints() throws Exception {
        RedeemRequest validReq = new RedeemRequest(50);
        when(customerService.redeemPoints("c1", 50)).thenReturn(new RedeemResponse(true, 10));

        mockMvc.perform(post("/api/v1/customers/c1/loyalty/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redeemed", is(true)))
                .andExpect(jsonPath("$.remainingPoints", is(10)));

        when(customerService.redeemPoints("c1", 100))
                .thenThrow(new BusinessRuleViolationException("Insufficient loyalty points"));

        mockMvc.perform(post("/api/v1/customers/c1/loyalty/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RedeemRequest(100))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }
}
