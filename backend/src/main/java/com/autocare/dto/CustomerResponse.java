package com.autocare.dto;

/**
 * Response payload representing a customer account.
 */
public record CustomerResponse(
        String id,
        String name,
        String phone,
        String email,
        int loyaltyPoints,
        int vehicleCount,
        String createdAt
) {
}
