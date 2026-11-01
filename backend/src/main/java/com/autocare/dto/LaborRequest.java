package com.autocare.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request payload for logging billable labor hours on a task.
 */
public record LaborRequest(
        @PositiveOrZero(message = "Billable hours must be non-negative")
        double billableHours
) {
}
