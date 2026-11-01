package com.autocare.mapper;

import com.autocare.domain.Invoice;
import com.autocare.dto.InvoiceResponse;
import com.autocare.dto.InvoiceSummary;
import com.autocare.dto.TaskResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper for converting Invoice aggregate root to InvoiceResponse and InvoiceSummary DTOs.
 */
@Component
public class InvoiceMapper {

    private final TaskMapper taskMapper;

    public InvoiceMapper(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        List<TaskResponse> tasks = invoice.getTaskList() != null
                ? invoice.getTaskList().stream().map(taskMapper::toResponse).toList()
                : Collections.emptyList();

        double subtotal = invoice.calculateSubtotal();
        double discount = invoice.getDiscountAmount();
        double taxableAmount = Math.max(0.0, subtotal - discount);
        double taxRate = invoice.getTaxRate();
        double taxAmount = (taxableAmount * taxRate) / 100.0;
        double total = invoice.calculateTotal();

        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getCustomer() != null ? invoice.getCustomer().getId() : null,
                invoice.getCustomer() != null ? invoice.getCustomer().getName() : null,
                invoice.getVehicle() != null ? invoice.getVehicle().getLicensePlate() : null,
                invoice.getVehicle() != null ? invoice.getVehicle().getVehicleType().toUpperCase() : null,
                invoice.getVehicle() != null ? invoice.getVehicle().getBaseServiceFee() : 0.0,
                tasks,
                subtotal,
                discount,
                taxRate,
                taxAmount,
                total,
                invoice.getStatus(),
                invoice.getIssuedAt() != null ? invoice.getIssuedAt().toString() : null,
                invoice.getSettledAt() != null ? invoice.getSettledAt().toString() : null
        );
    }

    public InvoiceSummary toSummary(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        return new InvoiceSummary(
                invoice.getInvoiceId(),
                invoice.getCustomer() != null ? invoice.getCustomer().getName() : null,
                invoice.getVehicle() != null ? invoice.getVehicle().getLicensePlate() : null,
                invoice.getStatus(),
                invoice.calculateTotal(),
                invoice.getIssuedAt() != null ? invoice.getIssuedAt().toString() : null
        );
    }
}
