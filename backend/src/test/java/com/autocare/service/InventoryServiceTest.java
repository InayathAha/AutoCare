package com.autocare.service;

import com.autocare.domain.InventoryItem;
import com.autocare.domain.Manager;
import com.autocare.domain.Mechanic;
import com.autocare.dto.InventoryItemRequest;
import com.autocare.dto.InventoryItemResponse;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.mapper.InventoryItemMapper;
import com.autocare.repository.EmployeeRepository;
import com.autocare.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private final InventoryItemMapper inventoryItemMapper = new InventoryItemMapper();

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryItemRepository, employeeRepository, inventoryItemMapper);
    }

    @Test
    @DisplayName("create() persists new inventory item when code is unique")
    void create_success() {
        InventoryItemRequest req = new InventoryItemRequest("OIL-5W30", "Engine Oil 5W30", 8.00, 20, 5);
        when(inventoryItemRepository.existsById("OIL-5W30")).thenReturn(false);

        InventoryItem saved = new InventoryItem("OIL-5W30", "Engine Oil 5W30", 8.00, 20, 5);
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(saved);

        InventoryItemResponse actual = inventoryService.create(req);

        assertThat(actual.partCode()).isEqualTo("OIL-5W30");
        assertThat(actual.partName()).isEqualTo("Engine Oil 5W30");
        assertThat(actual.unitPrice()).isEqualTo(8.00);
        assertThat(actual.stockQuantity()).isEqualTo(20);
        assertThat(actual.reorderThreshold()).isEqualTo(5);
        assertThat(actual.lowStock()).isFalse();
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    @DisplayName("create() throws DuplicateResourceException on duplicate part code")
    void create_duplicateCode_throws() {
        InventoryItemRequest req = new InventoryItemRequest("OIL-5W30", "Engine Oil 5W30", 8.00, 20, 5);
        when(inventoryItemRepository.existsById("OIL-5W30")).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.create(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("OIL-5W30");
    }

    @Test
    @DisplayName("update() modifies name, unit price, and reorder threshold without modifying stock")
    void update_success() {
        InventoryItem item = new InventoryItem("OIL-5W30", "Engine Oil Old", 7.50, 20, 5);
        when(inventoryItemRepository.findById("OIL-5W30")).thenReturn(Optional.of(item));

        InventoryItemRequest req = new InventoryItemRequest("OIL-5W30", "Engine Oil New", 9.00, 100, 10);
        InventoryItemResponse actual = inventoryService.update("OIL-5W30", req);

        assertThat(actual.partName()).isEqualTo("Engine Oil New");
        assertThat(actual.unitPrice()).isEqualTo(9.00);
        assertThat(actual.reorderThreshold()).isEqualTo(10);
        assertThat(actual.stockQuantity()).isEqualTo(20); // stock unchanged by update DTO
        assertThat(item.getPartName()).isEqualTo("Engine Oil New");
        assertThat(item.getUnitPrice()).isEqualTo(9.00);
        assertThat(item.getReorderThreshold()).isEqualTo(10);
        assertThat(item.getStockQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("restock() increments stock and updates lastRestockedAt via domain method")
    void restock_success() {
        InventoryItem item = new InventoryItem("OIL-5W30", "Engine Oil", 8.00, 10, 5);
        when(inventoryItemRepository.findById("OIL-5W30")).thenReturn(Optional.of(item));

        InventoryItemResponse actual = inventoryService.restock("OIL-5W30", 15);

        assertThat(actual.stockQuantity()).isEqualTo(25);
        assertThat(actual.lastRestockedAt()).isNotNull();
        assertThat(item.getStockQuantity()).isEqualTo(25);
        assertThat(item.getLastRestockedAt()).isNotNull();
    }

    @Test
    @DisplayName("generateReport() invokes manager report generation")
    void generateReport_manager_success() {
        Manager manager = new Manager("mgr1", "Carol", "0771234567", "carol@test.com", 2000.0, 300.0);
        when(employeeRepository.findById("mgr1")).thenReturn(Optional.of(manager));

        InventoryItem lowStockItem = new InventoryItem("FILTER", "Air Filter", 15.00, 2, 5);
        when(inventoryItemRepository.findAll()).thenReturn(List.of(lowStockItem));

        String report = inventoryService.generateReport("mgr1");

        assertThat(report).contains("LOW STOCK INVENTORY REPORT");
        assertThat(report).contains("FILTER");
    }

    @Test
    @DisplayName("generateReport() throws BusinessRuleViolationException when non-manager attempts report generation")
    void generateReport_nonManager_throws() {
        Mechanic mechanic = new Mechanic("m1", "Bob", "0771234567", "bob@test.com", 1000.0, "Engine", 15.0);
        when(employeeRepository.findById("m1")).thenReturn(Optional.of(mechanic));

        assertThatThrownBy(() -> inventoryService.generateReport("m1"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("MANAGER");
    }
}
