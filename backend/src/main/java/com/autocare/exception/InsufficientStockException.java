package com.autocare.exception;

/**
 * Thrown when an inventory deduction cannot be satisfied due to low stock.
 * Corresponds to HTTP 409 Conflict.
 */
public class InsufficientStockException extends AutoCareException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
