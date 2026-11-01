package com.autocare.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("VehicleTypeController Web Layer Tests")
class VehicleTypeControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new VehicleTypeController())
            .build();

    @Test
    @DisplayName("GET /api/v1/vehicle-types - returns polymorphic vehicle type info")
    void getVehicleTypes_success() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].type", is("CAR")))
                .andExpect(jsonPath("$[0].baseServiceFee", is(50.0)))
                .andExpect(jsonPath("$[1].type", is("VAN")))
                .andExpect(jsonPath("$[1].baseServiceFee", is(85.0)))
                .andExpect(jsonPath("$[2].type", is("MOTORCYCLE")))
                .andExpect(jsonPath("$[2].baseServiceFee", is(30.0)));
    }
}
