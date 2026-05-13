package com.mediflow.platform.doctor.exception;

import com.mediflow.platform.common.exception.ResourceAlreadyExistsException;

/**
 * Thrown when a doctor registration or update conflicts with an existing record.
 * Extends ResourceAlreadyExistsException so the GlobalExceptionHandler maps it to HTTP 409.
 *
 * Usage examples:
 *   throw new DoctorAlreadyExistsException("email", "doctor@hospital.com");
 *   throw new DoctorAlreadyExistsException("license number", "MH-12345");
 */
public class DoctorAlreadyExistsException extends ResourceAlreadyExistsException {

    public DoctorAlreadyExistsException(String field, String value) {
        super("A doctor with the " + field + " '" + value + "' is already registered in the system");
    }
}
