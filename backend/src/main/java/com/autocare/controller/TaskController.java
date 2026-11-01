package com.autocare.controller;

import com.autocare.dto.DiagnosticsRequest;
import com.autocare.dto.LaborRequest;
import com.autocare.dto.TaskPartRequest;
import com.autocare.dto.TaskRequest;
import com.autocare.dto.TaskResponse;
import com.autocare.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller managing service tasks, status workflow, diagnostics, and part/labor consumption.
 * Base path: /api/v1
 */
@RestController
@RequestMapping("/api/v1")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/invoices/{invoiceId}/tasks")
    public ResponseEntity<TaskResponse> createTask(@PathVariable("invoiceId") String invoiceId,
                                                   @Valid @RequestBody TaskRequest req) {
        TaskResponse response = taskService.create(invoiceId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tasks/{id}")
                .buildAndExpand(response.taskId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> listTasks(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "mechanicId", required = false) String mechanicId) {
        return ResponseEntity.ok(taskService.list(status, mechanicId));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable("id") String id) {
        return ResponseEntity.ok(taskService.get(id));
    }

    @PostMapping("/tasks/{id}/start")
    public ResponseEntity<TaskResponse> startTask(@PathVariable("id") String id) {
        return ResponseEntity.ok(taskService.start(id));
    }

    @PostMapping("/tasks/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(@PathVariable("id") String id) {
        return ResponseEntity.ok(taskService.complete(id));
    }

    @PatchMapping("/tasks/{id}/diagnostics")
    public ResponseEntity<TaskResponse> updateDiagnostics(@PathVariable("id") String id,
                                                          @Valid @RequestBody DiagnosticsRequest req) {
        return ResponseEntity.ok(taskService.updateDiagnostics(id, req));
    }

    @PostMapping("/tasks/{id}/parts")
    public ResponseEntity<TaskResponse> addPart(@PathVariable("id") String id,
                                                @Valid @RequestBody TaskPartRequest req) {
        return ResponseEntity.ok(taskService.addPart(id, req));
    }

    @PostMapping("/tasks/{id}/labor")
    public ResponseEntity<TaskResponse> recordLabor(@PathVariable("id") String id,
                                                    @Valid @RequestBody LaborRequest req) {
        return ResponseEntity.ok(taskService.recordLabor(id, req));
    }
}
