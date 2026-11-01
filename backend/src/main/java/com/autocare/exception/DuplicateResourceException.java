package com.autocare.exception;

/**
 * Thrown when attempting to create or register an entity that violates uniqueness constraints
 * (e.g. duplicate license plate or email address).
 * Corresponds to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends AutoCareException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
