package com.autocare.domain;

/**
 * Invoice lifecycle payment status.
 * Allowed forward progression: OPEN -> ISSUED -> PAID.
 */
public enum InvoiceStatus {
    OPEN,
    ISSUED,
    PAID
}
