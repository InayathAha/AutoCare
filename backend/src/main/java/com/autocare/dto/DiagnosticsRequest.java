package com.autocare.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for updating diagnostics description.
 */
public record DiagnosticsRequest(
        @NotBlank(message = "Diagnostics description cannot be blank")
        String description
) {
}
