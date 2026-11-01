package com.autocare.controller;

import com.autocare.dto.*;
import com.autocare.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing customers and registered vehicles.
 * Base path: /api/v1
 */
@RestController
@RequestMapping("/api/v1")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest req) {
        CustomerResponse response = customerService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> listCustomers(@RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(customerService.list(query));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable("id") String id) {
        return ResponseEntity.ok(customerService.get(id));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable("id") String id,
                                                           @Valid @RequestBody CustomerRequest req) {
        return ResponseEntity.ok(customerService.update(id, req));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("id") String id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/customers/{id}/vehicles")
    public ResponseEntity<VehicleResponse> registerVehicle(@PathVariable("id") String customerId,
                                                           @Valid @RequestBody VehicleRequest req) {
        VehicleResponse response = customerService.registerVehicle(customerId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/vehicles/{plate}")
                .buildAndExpand(response.licensePlate())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/customers/{id}/vehicles")
    public ResponseEntity<List<VehicleResponse>> getCustomerVehicles(@PathVariable("id") String customerId) {
        return ResponseEntity.ok(customerService.getCustomerVehicles(customerId));
    }

    @GetMapping("/vehicles/{licensePlate}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable("licensePlate") String licensePlate) {
        return ResponseEntity.ok(customerService.getVehicle(licensePlate));
    }

    @GetMapping("/customers/{id}/service-history")
    public ResponseEntity<List<InvoiceSummary>> getServiceHistory(@PathVariable("id") String customerId) {
        return ResponseEntity.ok(customerService.serviceHistory(customerId));
    }

    @PostMapping("/customers/{id}/loyalty/redeem")
    public ResponseEntity<RedeemResponse> redeemPoints(@PathVariable("id") String customerId,
                                                       @Valid @RequestBody RedeemRequest req) {
        return ResponseEntity.ok(customerService.redeemPoints(customerId, req.points()));
    }
}
