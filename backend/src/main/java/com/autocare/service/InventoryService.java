package com.autocare.service;

import com.autocare.domain.Employee;
import com.autocare.domain.InventoryItem;
import com.autocare.domain.Manager;
import com.autocare.dto.InventoryItemRequest;
import com.autocare.dto.InventoryItemResponse;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.mapper.InventoryItemMapper;
import com.autocare.repository.EmployeeRepository;
import com.autocare.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service orchestrating spare parts inventory management use cases.
 * Invariant: Stock mutations are only performed via guarded restock() and deductStock() methods.
 */
@Service
@Transactional
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final EmployeeRepository employeeRepository;
    private final InventoryItemMapper inventoryItemMapper;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            EmployeeRepository employeeRepository,
                            InventoryItemMapper inventoryItemMapper) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.employeeRepository = employeeRepository;
        this.inventoryItemMapper = inventoryItemMapper;
    }

    public InventoryItemResponse create(InventoryItemRequest req) {
        String code = req.partCode().trim().toUpperCase();
        if (inventoryItemRepository.existsById(code)) {
            throw new DuplicateResourceException("Inventory item with part code " + code + " already exists");
        }
        InventoryItem item = new InventoryItem(code, req.partName(), req.unitPrice(), req.stockQuantity(), req.reorderThreshold());
        InventoryItem saved = inventoryItemRepository.save(item);
        return inventoryItemMapper.toResponse(saved);
    }

    public InventoryItemResponse update(String partCode, InventoryItemRequest req) {
        InventoryItem item = findItemOrThrow(partCode);
        item.setPartName(req.partName());
        item.setUnitPrice(req.unitPrice());
        item.setReorderThreshold(req.reorderThreshold());
        // Note: stockQuantity is NOT modified directly here; protected by restock/deduct invariants
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse get(String partCode) {
        return inventoryItemMapper.toResponse(findItemOrThrow(partCode));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> list() {
        return inventoryItemRepository.findAll().stream()
                .map(inventoryItemMapper::toResponse)
                .toList();
    }

    public InventoryItemResponse restock(String partCode, int quantity) {
        InventoryItem item = findItemOrThrow(partCode);
        item.restock(quantity); // Invariant validation inside domain class
        return inventoryItemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findLowStock() {
        return inventoryItemRepository.findLowStock().stream()
                .map(inventoryItemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public String generateReport(String managerId) {
        Employee employee = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + managerId));

        if (!(employee instanceof Manager manager)) {
            throw new BusinessRuleViolationException("Only employees with MANAGER role can generate inventory reports");
        }

        List<InventoryItem> allItems = inventoryItemRepository.findAll();
        return manager.generateInventoryReport(allItems);
    }

    private InventoryItem findItemOrThrow(String partCode) {
        String code = partCode.trim().toUpperCase();
        return inventoryItemRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + code));
    }
}
