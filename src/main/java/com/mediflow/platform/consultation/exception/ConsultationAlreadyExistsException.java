package com.mediflow.platform.consultation.exception;

import com.mediflow.platform.common.exception.ResourceAlreadyExistsException;

public class ConsultationAlreadyExistsException extends ResourceAlreadyExistsException {

    public ConsultationAlreadyExistsException(String appointmentCode, String consultationCode) {
        super("A consultation already exists for appointment " + appointmentCode +
              ". Consultation code: " + consultationCode);
    }
}
