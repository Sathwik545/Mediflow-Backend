package com.mediflow.platform.consultation.exception;

import com.mediflow.platform.common.exception.ResourceNotFoundException;

public class ConsultationNotFoundException extends ResourceNotFoundException {

    public ConsultationNotFoundException(String consultationCode) {
        super("Consultation not found with code: " + consultationCode);
    }
}
