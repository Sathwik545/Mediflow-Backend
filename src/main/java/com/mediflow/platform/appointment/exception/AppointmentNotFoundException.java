package com.mediflow.platform.appointment.exception;

import com.mediflow.platform.common.exception.ResourceNotFoundException;

public class AppointmentNotFoundException extends ResourceNotFoundException {

    public AppointmentNotFoundException(String appointmentCode) {
        super("Appointment not found with code: " + appointmentCode);
    }
}
