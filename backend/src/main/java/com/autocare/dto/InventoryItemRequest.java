package com.autocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or editing an inventory part.
 * Note: stockQuantity is honoured on creation only; subsequent changes go through restock/deduct.
 */
public record InventoryItemRequest(
        @NotBlank(message = "Part code is required")
        @Size(max = 50, message = "Part code must not exceed 50 characters")
        String partCode,

        @NotBlank(message = "Part name is required")
        @Size(max = 100, message = "Part name must not exceed 100 characters")
        String partName,

        @PositiveOrZero(message = "Unit price must be non-negative")
        double unitPrice,

        @PositiveOrZero(message = "Stock quantity must be non-negative")
        int stockQuantity,

        @PositiveOrZero(message = "Reorder threshold must be non-negative")
        int reorderThreshold
) {
}
