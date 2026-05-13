package com.mediflow.platform.appointment.exception;

import com.mediflow.platform.common.exception.ResourceAlreadyExistsException;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentConflictException extends ResourceAlreadyExistsException {

    public AppointmentConflictException(String doctorCode, LocalDate date, LocalTime start, LocalTime end) {
        super(String.format(
            "Doctor %s already has an appointment on %s between %s and %s. Please choose a different time slot.",
            doctorCode, date, start, end
        ));
    }
}
