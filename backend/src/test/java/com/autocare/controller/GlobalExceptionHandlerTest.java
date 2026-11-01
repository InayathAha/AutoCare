package com.autocare.controller;

import com.autocare.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler - Error Code Reachability Acceptance Suite")
class GlobalExceptionHandlerTest {

    @RestController
    static class TestFaultController {

        @GetMapping("/fault")
        public void throwFault(@RequestParam("code") String code) {
            switch (code) {
                case "NOT_FOUND" -> throw new ResourceNotFoundException("Resource missing");
                case "DUPLICATE_RESOURCE" -> throw new DuplicateResourceException("Resource already exists");
                case "INSUFFICIENT_STOCK" -> throw new InsufficientStockException("Insufficient stock available");
                case "INVALID_STATE_TRANSITION" -> throw new InvalidStateTransitionException("State transition forbidden");
                case "INVALID_DISCOUNT" -> throw new InvalidDiscountException("Discount terms violated");
                case "BUSINESS_RULE_VIOLATION" -> throw new BusinessRuleViolationException("Domain rule violated");
                case "FK_CONSTRAINT" -> throw new DataIntegrityViolationException("Foreign key restriction");
                case "INTERNAL_ERROR" -> throw new RuntimeException("Unexpected fatal failure");
                case "ILLEGAL_ARG" -> throw new IllegalArgumentException("Bad input parameter");
                default -> throw new RuntimeException("Unknown fault");
            }
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestFaultController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("NOT_FOUND -> 404 with code NOT_FOUND")
    void testNotFound() throws Exception {
        mockMvc.perform(get("/fault").param("code", "NOT_FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path", is("/fault")));
    }

    @Test
    @DisplayName("DUPLICATE_RESOURCE -> 409 with code DUPLICATE_RESOURCE")
    void testDuplicateResource() throws Exception {
        mockMvc.perform(get("/fault").param("code", "DUPLICATE_RESOURCE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("INSUFFICIENT_STOCK -> 409 with code INSUFFICIENT_STOCK")
    void testInsufficientStock() throws Exception {
        mockMvc.perform(get("/fault").param("code", "INSUFFICIENT_STOCK"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("INVALID_STATE_TRANSITION -> 409 with code INVALID_STATE_TRANSITION")
    void testInvalidStateTransition() throws Exception {
        mockMvc.perform(get("/fault").param("code", "INVALID_STATE_TRANSITION"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_STATE_TRANSITION")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("FK_CONSTRAINT -> 409 with code FK_CONSTRAINT")
    void testFkConstraint() throws Exception {
        mockMvc.perform(get("/fault").param("code", "FK_CONSTRAINT"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("FK_CONSTRAINT")))
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("INVALID_DISCOUNT -> 422 with code INVALID_DISCOUNT")
    void testInvalidDiscount() throws Exception {
        mockMvc.perform(get("/fault").param("code", "INVALID_DISCOUNT"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("INVALID_DISCOUNT")))
                .andExpect(jsonPath("$.status", is(422)));
    }

    @Test
    @DisplayName("BUSINESS_RULE_VIOLATION -> 422 with code BUSINESS_RULE_VIOLATION")
    void testBusinessRuleViolation() throws Exception {
        mockMvc.perform(get("/fault").param("code", "BUSINESS_RULE_VIOLATION"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")))
                .andExpect(jsonPath("$.status", is(422)));
    }

    @Test
    @DisplayName("INTERNAL_ERROR -> 500 with code INTERNAL_ERROR")
    void testInternalError() throws Exception {
        mockMvc.perform(get("/fault").param("code", "INTERNAL_ERROR"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
                .andExpect(jsonPath("$.status", is(500)));
    }

    @Test
    @DisplayName("ILLEGAL_ARG -> 400 with code VALIDATION_FAILED")
    void testIllegalArgument() throws Exception {
        mockMvc.perform(get("/fault").param("code", "ILLEGAL_ARG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.status", is(400)));
    }
}
