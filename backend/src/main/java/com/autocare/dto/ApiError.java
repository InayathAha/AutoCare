package com.autocare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard API error payload returned on non-2xx responses.
 * Matches docs/04-api-contract.md §2.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {
    public ApiError(String timestamp, int status, String error, String code, String message, String path) {
        this(timestamp, status, error, code, message, path, null);
    }
}
