package com.autocare.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for opening a new invoice for a customer's vehicle.
 */
public record OpenInvoiceRequest(
        @NotBlank(message = "Customer ID is required")
        String customerId,

        @NotBlank(message = "License plate is required")
        String licensePlate
) {
}
