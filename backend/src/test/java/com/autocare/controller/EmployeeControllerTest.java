package com.autocare.controller;

import com.autocare.dto.EmployeeRequest;
import com.autocare.dto.EmployeeResponse;
import com.autocare.dto.OvertimeRequest;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.GlobalExceptionHandler;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.service.EmployeeService;
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
@DisplayName("EmployeeController Web Layer Tests")
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        EmployeeController controller = new EmployeeController(employeeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/employees - 201 Created on valid Mechanic")
    void createEmployee_success() throws Exception {
        EmployeeRequest req = new EmployeeRequest("MECHANIC", "Bob Engine", "0771234567", "bob@test.com", 1000.0, "Engine", 15.0, null);
        EmployeeResponse res = new EmployeeResponse("m1", "MECHANIC", "Bob Engine", "0771234567", "bob@test.com", 1000.0, 1000.0, "Engine", 0, 15.0, null);

        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", is("m1")))
                .andExpect(jsonPath("$.type", is("MECHANIC")));
    }

    @Test
    @DisplayName("POST /api/v1/employees - 400 VALIDATION_FAILED on invalid email/salary")
    void createEmployee_validationFailed() throws Exception {
        EmployeeRequest req = new EmployeeRequest("MECHANIC", "", "0771", "invalid-email", -500.0, null, null, null);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/employees - 409 DUPLICATE_RESOURCE on duplicate email")
    void createEmployee_duplicateEmail() throws Exception {
        EmployeeRequest req = new EmployeeRequest("MECHANIC", "Bob", "0771234567", "bob@test.com", 1000.0, "Engine", 15.0, null);
        when(employeeService.create(any(EmployeeRequest.class)))
                .thenThrow(new DuplicateResourceException("An employee with email bob@test.com already exists"));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_RESOURCE")));
    }

    @Test
    @DisplayName("GET /api/v1/employees - 200 OK list with query params")
    void listEmployees_success() throws Exception {
        EmployeeResponse res = new EmployeeResponse("m1", "MECHANIC", "Bob", "0771", "b@t.com", 1000.0, 1000.0, "Engine", 0, 15.0, null);
        when(employeeService.list("MECHANIC", "Engine")).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/employees")
                        .param("role", "MECHANIC")
                        .param("specialization", "Engine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specialization", is("Engine")));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} - 200 OK when exists, 404 when missing")
    void getEmployee_endpoints() throws Exception {
        EmployeeResponse res = new EmployeeResponse("m1", "MECHANIC", "Bob", "0771", "b@t.com", 1000.0, 1000.0, null, 0, 15.0, null);
        when(employeeService.get("m1")).thenReturn(res);
        when(employeeService.get("unknown")).thenThrow(new ResourceNotFoundException("Employee not found: unknown"));

        mockMvc.perform(get("/api/v1/employees/m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("m1")));

        mockMvc.perform(get("/api/v1/employees/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} - 200 OK and 422 BUSINESS_RULE_VIOLATION on role mutation")
    void updateEmployee_endpoints() throws Exception {
        EmployeeRequest validReq = new EmployeeRequest("MECHANIC", "Bob Updated", "0771234567", "bob@test.com", 1200.0, "Engine", 18.0, null);
        EmployeeResponse res = new EmployeeResponse("m1", "MECHANIC", "Bob Updated", "0771234567", "bob@test.com", 1200.0, 1200.0, "Engine", 0, 18.0, null);
        when(employeeService.update(eq("m1"), any(EmployeeRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/v1/employees/m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Bob Updated")));

        when(employeeService.update(eq("m1"), any(EmployeeRequest.class)))
                .thenThrow(new BusinessRuleViolationException("Employee role cannot be modified after creation"));

        mockMvc.perform(put("/api/v1/employees/m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")));
    }

    @Test
    @DisplayName("POST /api/v1/mechanics/{id}/overtime - 200 OK on success and 400 on negative hours")
    void logOvertime_endpoints() throws Exception {
        OvertimeRequest validReq = new OvertimeRequest(5);
        EmployeeResponse res = new EmployeeResponse("m1", "MECHANIC", "Bob", "0771", "b@t.com", 1000.0, 1075.0, "Engine", 5, 15.0, null);
        when(employeeService.logOvertime("m1", 5)).thenReturn(res);

        mockMvc.perform(post("/api/v1/mechanics/m1/overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overtimeHours", is(5)));

        OvertimeRequest invalidReq = new OvertimeRequest(-2);
        mockMvc.perform(post("/api/v1/mechanics/m1/overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }
}
