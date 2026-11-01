package com.autocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request payload for attaching consumed parts to a service task.
 */
public record TaskPartRequest(
        @NotBlank(message = "Part code is required")
        String partCode,

        @Positive(message = "Quantity must be greater than 0")
        int quantity
) {
}
