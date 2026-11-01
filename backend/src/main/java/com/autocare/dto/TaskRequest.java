package com.autocare.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a service task (job card).
 */
public record TaskRequest(
        @NotBlank(message = "Task description is required")
        String description,

        @NotBlank(message = "Assigned mechanic ID is required")
        String assignedMechanicId
) {
}
