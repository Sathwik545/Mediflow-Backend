package com.mediflow.platform.common.exception;

/**
 * Thrown when a business rule or domain invariant is violated.
 *
 * Examples: booking an appointment for an inactive patient, attempting an
 * invalid status transition, or deactivating a doctor with future appointments.
 *
 * Mapped to HTTP 422 Unprocessable Entity by {@link GlobalExceptionHandler}.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
