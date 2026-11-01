package com.autocare.exception;

/**
 * Thrown when an entity lookup fails (e.g. unknown customer, vehicle, employee, or part).
 * Corresponds to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends AutoCareException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
