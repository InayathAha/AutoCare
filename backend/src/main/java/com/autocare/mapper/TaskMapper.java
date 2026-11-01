package com.autocare.mapper;

import com.autocare.domain.ServiceTask;
import com.autocare.domain.TaskUsedPart;
import com.autocare.dto.TaskResponse;
import com.autocare.dto.UsedPartResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper for converting ServiceTask and TaskUsedPart entities to TaskResponse and UsedPartResponse DTOs.
 */
@Component
public class TaskMapper {

    public TaskResponse toResponse(ServiceTask task) {
        if (task == null) {
            return null;
        }

        List<UsedPartResponse> parts = task.getUsedParts() != null
                ? task.getUsedParts().stream().map(this::toUsedPartResponse).toList()
                : Collections.emptyList();

        return new TaskResponse(
                task.getTaskId(),
                task.getInvoice() != null ? task.getInvoice().getInvoiceId() : null,
                task.getDescription(),
                task.getTaskStatus().name(),
                task.getAssignedMechanic() != null ? task.getAssignedMechanic().getId() : null,
                task.getAssignedMechanic() != null ? task.getAssignedMechanic().getName() : null,
                task.getBillableHours(),
                task.getLaborCharge(),
                task.getPartsCost(),
                task.calculateTaskCost(),
                parts,
                task.getCreatedAt() != null ? task.getCreatedAt().toString() : null
        );
    }

    public UsedPartResponse toUsedPartResponse(TaskUsedPart part) {
        if (part == null) {
            return null;
        }
        return new UsedPartResponse(
                part.getItem() != null ? part.getItem().getPartCode() : null,
                part.getItem() != null ? part.getItem().getPartName() : null,
                part.getQuantity(),
                part.getUnitPrice(),
                part.getSubtotal()
        );
    }
}
