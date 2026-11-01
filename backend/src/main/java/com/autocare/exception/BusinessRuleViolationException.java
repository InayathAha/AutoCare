package com.autocare.exception;

/**
 * Thrown when an operation violates garage business rules, such as completing
 * a task assigned to another mechanic or mutating an issued/settled invoice.
 * Corresponds to HTTP 422 Unprocessable Content.
 */
public class BusinessRuleViolationException extends AutoCareException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
