package com.autocare.repository;

import com.autocare.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * Finds an inventory item with a pessimistic write lock (FOR UPDATE)
     * to prevent race conditions during concurrent stock deductions.
     *
     * @param partCode part code
     * @return optional item with row lock held
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.partCode = :partCode")
    Optional<InventoryItem> findByPartCodeForUpdate(@Param("partCode") String partCode);
}
