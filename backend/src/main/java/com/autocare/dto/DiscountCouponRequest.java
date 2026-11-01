package com.autocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request payload for applying fixed coupon discount (Overload 2).
 */
public record DiscountCouponRequest(
        @NotBlank(message = "Coupon code is required")
        String couponCode,

        @Positive(message = "Fixed reduction must be strictly positive")
        double fixedReduction
) {
}
