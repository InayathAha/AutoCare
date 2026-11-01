package com.autocare.dto;

import java.util.List;

/**
 * Response payload representing a service task (job card).
 */
public record TaskResponse(
        String taskId,
        String invoiceId,
        String description,
        String status,
        String assignedMechanicId,
        String assignedMechanicName,
        double billableHours,
        double laborCharge,
        double partsCost,
        double totalTaskCost,
        List<UsedPartResponse> usedParts,
        String createdAt
) {
}
