package com.mediflow.platform.common.exception;

/**
 * Base exception for all duplicate-record / conflict scenarios across the platform.
 *
 * Every domain module should define its own subclass (e.g., PatientAlreadyExistsException,
 * DoctorAlreadyExistsException) that extends this class with a specific message.
 *
 * The {@link GlobalExceptionHandler} maps this to HTTP 409 Conflict, so
 * individual handlers per domain are no longer needed.
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
