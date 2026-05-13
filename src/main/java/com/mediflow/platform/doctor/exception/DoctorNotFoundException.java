package com.mediflow.platform.doctor.exception;

import com.mediflow.platform.common.exception.ResourceNotFoundException;

/**
 * Thrown when a doctor lookup by doctorCode yields no result.
 * Extends ResourceNotFoundException so the GlobalExceptionHandler maps it to HTTP 404.
 */
public class DoctorNotFoundException extends ResourceNotFoundException {

    public DoctorNotFoundException(String doctorCode) {
        super("No doctor found with doctor code: " + doctorCode);
    }
}
