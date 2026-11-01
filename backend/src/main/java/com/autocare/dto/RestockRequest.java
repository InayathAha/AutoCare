package com.autocare.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request payload for restocking inventory parts.
 */
public record RestockRequest(
        @Positive(message = "Restock quantity must be greater than 0")
        int quantity
) {
}
