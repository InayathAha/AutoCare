package com.autocare.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request payload for logging mechanic overtime hours.
 */
public record OvertimeRequest(
        @Positive(message = "Overtime hours must be greater than 0")
        int hours
) {
}
