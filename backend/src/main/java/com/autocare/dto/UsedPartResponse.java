package com.autocare.dto;

/**
 * Response payload representing a part consumed in a service task.
 * unitPrice captures the snapshot price at consumption time.
 */
public record UsedPartResponse(
        String partCode,
        String partName,
        int quantity,
        double unitPrice,
        double subtotal
) {
}
