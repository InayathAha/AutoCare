package com.autocare.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request payload for redeeming customer loyalty points.
 */
public record RedeemRequest(
        @Positive(message = "Points to redeem must be greater than 0")
        int points
) {
}
