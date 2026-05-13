package com.mediflow.platform.common.exception;

/**
 * Base exception for all "resource not found" scenarios across the platform.
 *
 * Every domain module should define its own subclass (e.g., PatientNotFoundException,
 * DoctorNotFoundException) that extends this class with a specific message.
 *
 * The {@link GlobalExceptionHandler} maps this to HTTP 404 Not Found, so
 * individual handlers per domain are no longer needed.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
