package com.autocare.dto;

/**
 * Summary response payload for invoice lists and customer service history.
 */
public record InvoiceSummary(
        String invoiceId,
        String customerName,
        String licensePlate,
        String status,
        double totalAmount,
        String issuedAt
) {
}
