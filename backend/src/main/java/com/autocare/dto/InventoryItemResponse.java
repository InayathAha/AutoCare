package com.autocare.dto;

/**
 * Response payload representing an inventory spare part.
 */
public record InventoryItemResponse(
        String partCode,
        String partName,
        double unitPrice,
        int stockQuantity,
        int reorderThreshold,
        boolean lowStock,
        String lastRestockedAt
) {
}
