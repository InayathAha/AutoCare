package com.autocare.repository;

import com.autocare.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for InventoryItem.
 * Manages spare parts inventory and low-stock detection.
 */
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Finds all items whose stock quantity has fallen to or below the reorder threshold.
     *
     * @return list of low-stock inventory items
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.stockQuantity <= i.reorderThreshold")
    List<InventoryItem> findLowStock();
}
