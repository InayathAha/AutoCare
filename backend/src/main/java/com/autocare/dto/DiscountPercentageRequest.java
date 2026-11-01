package com.autocare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Request payload for applying percentage discount (Overload 1).
 */
public record DiscountPercentageRequest(
        @DecimalMin(value = "0.01", message = "Percentage must be strictly positive")
        @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100")
        double percentage,

        String managerId
) {
}
