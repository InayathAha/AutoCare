package com.autocare.mapper;

import com.autocare.domain.InventoryItem;
import com.autocare.dto.InventoryItemResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting InventoryItem entities to InventoryItemResponse DTOs.
 */
@Component
public class InventoryItemMapper {

    public InventoryItemResponse toResponse(InventoryItem item) {
        if (item == null) {
            return null;
        }
        return new InventoryItemResponse(
                item.getPartCode(),
                item.getPartName(),
                item.getUnitPrice(),
                item.getStockQuantity(),
                item.getReorderThreshold(),
                item.isLowStock(),
                item.getLastRestockedAt() != null ? item.getLastRestockedAt().toString() : null
        );
    }
}
