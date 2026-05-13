package com.mediflow.platform.patient.exception;

import com.mediflow.platform.common.exception.ResourceNotFoundException;

/**
 * Thrown when a patient lookup by patientCode yields no result.
 * Extends ResourceNotFoundException so the GlobalExceptionHandler maps it to HTTP 404.
 */
public class PatientNotFoundException extends ResourceNotFoundException {

    public PatientNotFoundException(String patientCode) {
        super("No patient found with patient code: " + patientCode);
    }
}
