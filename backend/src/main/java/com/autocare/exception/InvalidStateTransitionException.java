package com.autocare.exception;

/**
 * Thrown when an illegal lifecycle state transition is attempted on a ServiceTask or Invoice.
 * Corresponds to HTTP 409 Conflict.
 */
public class InvalidStateTransitionException extends AutoCareException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
