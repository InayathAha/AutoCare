package com.autocare.controller;

import com.autocare.dto.InventoryItemRequest;
import com.autocare.dto.InventoryItemResponse;
import com.autocare.dto.ReportResponse;
import com.autocare.dto.RestockRequest;
import com.autocare.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing spare parts inventory, stock tracking, and manager reports.
 * Base path: /api/v1
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryItemResponse> createItem(@Valid @RequestBody InventoryItemRequest req) {
        InventoryItemResponse response = inventoryService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{partCode}")
                .buildAndExpand(response.partCode())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InventoryItemResponse>> listItems() {
        return ResponseEntity.ok(inventoryService.list());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItemResponse>> listLowStock() {
        return ResponseEntity.ok(inventoryService.findLowStock());
    }

    @GetMapping("/report")
    public ResponseEntity<ReportResponse> generateReport(@RequestParam("managerId") String managerId) {
        String report = inventoryService.generateReport(managerId);
        return ResponseEntity.ok(new ReportResponse(report));
    }

    @GetMapping("/{partCode}")
    public ResponseEntity<InventoryItemResponse> getItem(@PathVariable("partCode") String partCode) {
        return ResponseEntity.ok(inventoryService.get(partCode));
    }

    @PutMapping("/{partCode}")
    public ResponseEntity<InventoryItemResponse> updateItem(@PathVariable("partCode") String partCode,
                                                            @Valid @RequestBody InventoryItemRequest req) {
        return ResponseEntity.ok(inventoryService.update(partCode, req));
    }

    @PostMapping("/{partCode}/restock")
    public ResponseEntity<InventoryItemResponse> restockItem(@PathVariable("partCode") String partCode,
                                                             @Valid @RequestBody RestockRequest req) {
        return ResponseEntity.ok(inventoryService.restock(partCode, req.quantity()));
    }
}
