package com.autocare.exception;

/**
 * Root unchecked exception for all domain and business rule violations in AutoCare.
 * <p>
 * Demonstrates exception hierarchy design in OOP: concrete domain exceptions
 * inherit from this base class, enabling centralized exception handling in the API layer.
 */
public class AutoCareException extends RuntimeException {

    public AutoCareException(String message) {
        super(message);
    }

    public AutoCareException(String message, Throwable cause) {
        super(message, cause);
    }
}
