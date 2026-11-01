package com.autocare.dto;

/**
 * Response payload after loyalty point redemption.
 */
public record RedeemResponse(
        boolean redeemed,
        int remainingPoints
) {
}
