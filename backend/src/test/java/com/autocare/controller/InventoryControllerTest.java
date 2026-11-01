package com.autocare.controller;

import com.autocare.dto.InventoryItemRequest;
import com.autocare.dto.InventoryItemResponse;
import com.autocare.dto.RestockRequest;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.GlobalExceptionHandler;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.service.InventoryService;
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

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryController Web Layer Tests")
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        InventoryController controller = new InventoryController(inventoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/inventory - 201 Created on valid input")
    void createItem_success() throws Exception {
        InventoryItemRequest req = new InventoryItemRequest("OIL-5W30", "Engine Oil", 8.00, 20, 5);
        InventoryItemResponse res = new InventoryItemResponse("OIL-5W30", "Engine Oil", 8.00, 20, 5, false, null);

        when(inventoryService.create(any(InventoryItemRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.partCode", is("OIL-5W30")));
    }

    @Test
    @DisplayName("POST /api/v1/inventory - 409 DUPLICATE_RESOURCE on existing code")
    void createItem_duplicate() throws Exception {
        InventoryItemRequest req = new InventoryItemRequest("OIL-5W30", "Engine Oil", 8.00, 20, 5);
        when(inventoryService.create(any(InventoryItemRequest.class)))
                .thenThrow(new DuplicateResourceException("Inventory item with part code OIL-5W30 already exists"));

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_RESOURCE")));
    }

    @Test
    @DisplayName("GET /api/v1/inventory - 200 OK list")
    void listItems_success() throws Exception {
        InventoryItemResponse res = new InventoryItemResponse("OIL-5W30", "Engine Oil", 8.00, 20, 5, false, null);
        when(inventoryService.list()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partCode", is("OIL-5W30")));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{partCode} - 200 OK when found, 404 when missing")
    void getItem_endpoints() throws Exception {
        InventoryItemResponse res = new InventoryItemResponse("OIL-5W30", "Engine Oil", 8.00, 20, 5, false, null);
        when(inventoryService.get("OIL-5W30")).thenReturn(res);
        when(inventoryService.get("UNKNOWN")).thenThrow(new ResourceNotFoundException("Inventory item not found: UNKNOWN"));

        mockMvc.perform(get("/api/v1/inventory/OIL-5W30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partCode", is("OIL-5W30")));

        mockMvc.perform(get("/api/v1/inventory/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/{partCode} - 200 OK on update")
    void updateItem_success() throws Exception {
        InventoryItemRequest req = new InventoryItemRequest("OIL-5W30", "Engine Oil New", 9.00, 20, 10);
        InventoryItemResponse res = new InventoryItemResponse("OIL-5W30", "Engine Oil New", 9.00, 20, 10, false, null);
        when(inventoryService.update(eq("OIL-5W30"), any(InventoryItemRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/v1/inventory/OIL-5W30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partName", is("Engine Oil New")));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/{partCode}/restock - 200 OK on success, 400 on non-positive quantity")
    void restockItem_endpoints() throws Exception {
        RestockRequest req = new RestockRequest(15);
        InventoryItemResponse res = new InventoryItemResponse("OIL-5W30", "Engine Oil", 8.00, 35, 5, false, "2026-11-02T00:00:00Z");
        when(inventoryService.restock("OIL-5W30", 15)).thenReturn(res);

        mockMvc.perform(post("/api/v1/inventory/OIL-5W30/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity", is(35)));

        RestockRequest invalidReq = new RestockRequest(0);
        mockMvc.perform(post("/api/v1/inventory/OIL-5W30/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/low-stock - 200 OK")
    void listLowStock_success() throws Exception {
        InventoryItemResponse res = new InventoryItemResponse("FILTER", "Air Filter", 15.00, 2, 5, true, null);
        when(inventoryService.findLowStock()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lowStock", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/report - 200 OK with report, 422 if employee is not manager")
    void generateReport_endpoints() throws Exception {
        when(inventoryService.generateReport("mgr1")).thenReturn("LOW STOCK INVENTORY REPORT");

        mockMvc.perform(get("/api/v1/inventory/report").param("managerId", "mgr1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report").value(org.hamcrest.Matchers.containsString("LOW STOCK")));

        when(inventoryService.generateReport("mech1"))
                .thenThrow(new BusinessRuleViolationException("Only employees with MANAGER role can generate inventory reports"));

        mockMvc.perform(get("/api/v1/inventory/report").param("managerId", "mech1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }
}
