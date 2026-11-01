package com.autocare.controller;

import com.autocare.dto.DiagnosticsRequest;
import com.autocare.dto.LaborRequest;
import com.autocare.dto.TaskPartRequest;
import com.autocare.dto.TaskRequest;
import com.autocare.dto.TaskResponse;
import com.autocare.exception.*;
import com.autocare.service.TaskService;
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
@DisplayName("TaskController Web Layer Tests")
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TaskController controller = new TaskController(taskService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{invoiceId}/tasks - 201 Created on valid input")
    void createTask_success() throws Exception {
        TaskRequest req = new TaskRequest("Oil change", "m1");
        TaskResponse res = new TaskResponse("t1", "inv1", "Oil change", "PENDING", "m1", "Bob", 0, 0, 0, 0, Collections.emptyList(), "2026-11-02T00:00:00Z");

        when(taskService.create(eq("inv1"), any(TaskRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/invoices/inv1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.taskId", is("t1")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/{invoiceId}/tasks - 404 NOT_FOUND on missing invoice")
    void createTask_invoiceNotFound() throws Exception {
        TaskRequest req = new TaskRequest("Oil change", "m1");
        when(taskService.create(eq("unknown"), any(TaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Invoice not found: unknown"));

        mockMvc.perform(post("/api/v1/invoices/unknown/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - 200 OK list with query params")
    void listTasks_success() throws Exception {
        TaskResponse res = new TaskResponse("t1", "inv1", "Oil change", "PENDING", "m1", "Bob", 0, 0, 0, 0, Collections.emptyList(), null);
        when(taskService.list("PENDING", "m1")).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/tasks")
                        .param("status", "PENDING")
                        .param("mechanicId", "m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId", is("t1")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} - 200 OK when found, 404 when missing")
    void getTask_endpoints() throws Exception {
        TaskResponse res = new TaskResponse("t1", "inv1", "Oil change", "PENDING", "m1", "Bob", 0, 0, 0, 0, Collections.emptyList(), null);
        when(taskService.get("t1")).thenReturn(res);
        when(taskService.get("unknown")).thenThrow(new ResourceNotFoundException("Service task not found: unknown"));

        mockMvc.perform(get("/api/v1/tasks/t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId", is("t1")));

        mockMvc.perform(get("/api/v1/tasks/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/start - 200 OK on success, 409 INVALID_STATE_TRANSITION on illegal move")
    void startTask_endpoints() throws Exception {
        TaskResponse res = new TaskResponse("t1", "inv1", "Oil change", "IN_PROGRESS", "m1", "Bob", 0, 0, 0, 0, Collections.emptyList(), null);
        when(taskService.start("t1")).thenReturn(res);

        mockMvc.perform(post("/api/v1/tasks/t1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        when(taskService.start("t2"))
                .thenThrow(new InvalidStateTransitionException("Invalid state transition: already completed"));

        mockMvc.perform(post("/api/v1/tasks/t2/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_STATE_TRANSITION")));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/complete - 200 OK on success, 422 BUSINESS_RULE_VIOLATION if mechanic mismatch")
    void completeTask_endpoints() throws Exception {
        TaskResponse res = new TaskResponse("t1", "inv1", "Oil change", "COMPLETED", "m1", "Bob", 0, 0, 0, 0, Collections.emptyList(), null);
        when(taskService.complete("t1")).thenReturn(res);

        mockMvc.perform(post("/api/v1/tasks/t1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        when(taskService.complete("t2"))
                .thenThrow(new BusinessRuleViolationException("Mechanic Carol is not assigned to task t2"));

        mockMvc.perform(post("/api/v1/tasks/t2/complete"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/diagnostics - 200 OK on success, 400 on blank description")
    void updateDiagnostics_endpoints() throws Exception {
        DiagnosticsRequest validReq = new DiagnosticsRequest("New findings");
        TaskResponse res = new TaskResponse("t1", "inv1", "New findings", "IN_PROGRESS", "m1", "Bob", 0, 0, 0, 0, Collections.emptyList(), null);
        when(taskService.updateDiagnostics(eq("t1"), any(DiagnosticsRequest.class))).thenReturn(res);

        mockMvc.perform(patch("/api/v1/tasks/t1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("New findings")));

        DiagnosticsRequest blankReq = new DiagnosticsRequest("");
        mockMvc.perform(patch("/api/v1/tasks/t1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/parts - 200 OK on success, 409 INSUFFICIENT_STOCK on shortage")
    void addPart_endpoints() throws Exception {
        TaskPartRequest validReq = new TaskPartRequest("OIL-5W30", 2);
        TaskResponse res = new TaskResponse("t1", "inv1", "Oil change", "IN_PROGRESS", "m1", "Bob", 0, 0, 16.0, 16.0, Collections.emptyList(), null);
        when(taskService.addPart(eq("t1"), any(TaskPartRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/tasks/t1/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partsCost", is(16.0)));

        when(taskService.addPart(eq("t1"), any(TaskPartRequest.class)))
                .thenThrow(new InsufficientStockException("Only 1 unit in stock, requested 5"));

        mockMvc.perform(post("/api/v1/tasks/t1/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskPartRequest("OIL-5W30", 5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/labor - 200 OK on success, 400 on negative hours")
    void recordLabor_endpoints() throws Exception {
        LaborRequest validReq = new LaborRequest(1.5);
        TaskResponse res = new TaskResponse("t1", "inv1", "Oil change", "IN_PROGRESS", "m1", "Bob", 1.5, 30.0, 0, 30.0, Collections.emptyList(), null);
        when(taskService.recordLabor(eq("t1"), any(LaborRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/tasks/t1/labor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billableHours", is(1.5)))
                .andExpect(jsonPath("$.laborCharge", is(30.0)));

        LaborRequest invalidReq = new LaborRequest(-1.0);
        mockMvc.perform(post("/api/v1/tasks/t1/labor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }
}
