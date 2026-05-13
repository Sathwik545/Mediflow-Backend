package com.mediflow.platform.patient.exception;

import com.mediflow.platform.common.exception.ResourceAlreadyExistsException;

/**
 * Thrown when a patient registration conflicts with an existing email address.
 * Extends ResourceAlreadyExistsException so the GlobalExceptionHandler maps it to HTTP 409.
 */
public class PatientAlreadyExistsException extends ResourceAlreadyExistsException {

    public PatientAlreadyExistsException(String email) {
        super("A patient with the email address '" + email + "' is already registered in the system");
    }
}
