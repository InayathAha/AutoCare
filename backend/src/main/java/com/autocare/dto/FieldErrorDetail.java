package com.autocare.dto;

/**
 * Details of field validation failure for 400 Bad Request responses.
 */
public record FieldErrorDetail(
        String field,
        String message
) {
}
