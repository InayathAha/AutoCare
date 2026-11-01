package com.autocare.dto;

import java.util.List;

/**
 * Detailed response payload representing a garage invoice.
 * Demonstrates polymorphic billing calculation across Vehicle subclasses and tasks.
 */
public record InvoiceResponse(
        String invoiceId,
        String customerId,
        String customerName,
        String licensePlate,
        String vehicleType,
        double baseServiceFee,
        List<TaskResponse> tasks,
        double subtotal,
        double discountAmount,
        double taxRate,
        double taxAmount,
        double totalAmount,
        String status,
        String issuedAt,
        String settledAt
) {
}
