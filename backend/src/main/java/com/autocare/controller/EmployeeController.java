package com.autocare.controller;

import com.autocare.dto.EmployeeRequest;
import com.autocare.dto.EmployeeResponse;
import com.autocare.dto.OvertimeRequest;
import com.autocare.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing garage employees, mechanics, and dispatching.
 * Base path: /api/v1
 */
@RestController
@RequestMapping("/api/v1")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest req) {
        EmployeeResponse response = employeeService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponse>> listEmployees(
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "specialization", required = false) String specialization) {
        return ResponseEntity.ok(employeeService.list(role, specialization));
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable("id") String id) {
        return ResponseEntity.ok(employeeService.get(id));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable("id") String id,
                                                           @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(employeeService.update(id, req));
    }

    @PostMapping("/mechanics/{id}/overtime")
    public ResponseEntity<EmployeeResponse> logOvertime(@PathVariable("id") String mechanicId,
                                                        @Valid @RequestBody OvertimeRequest req) {
        return ResponseEntity.ok(employeeService.logOvertime(mechanicId, req.hours()));
    }
}
