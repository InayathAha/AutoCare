package com.autocare.exception;

/**
 * Thrown when an invalid discount percentage, coupon code, or unauthorized discount is applied.
 * Corresponds to HTTP 422 Unprocessable Content.
 */
public class InvalidDiscountException extends AutoCareException {

    public InvalidDiscountException(String message) {
        super(message);
    }

    public InvalidDiscountException(String message, Throwable cause) {
        super(message, cause);
    }
}
