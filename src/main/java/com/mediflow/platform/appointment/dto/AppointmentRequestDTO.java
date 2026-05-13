package com.mediflow.platform.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.platform.appointment.enums.BookedBy;
import com.mediflow.platform.appointment.enums.ConsultationType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {

    @NotBlank(message = "Patient code is required")
    private String patientCode;

    @NotBlank(message = "Doctor code is required")
    private String doctorCode;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be today or a future date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotNull(message = "Consultation type is required")
    private ConsultationType consultationType;

    // Optional — backend derives this from the authenticated JWT principal.
    // ADMIN role → ADMIN, PATIENT role → PATIENT, anything else → SYSTEM.
    private BookedBy bookedBy;

    @Size(max = 1000, message = "Reason for visit must not exceed 1000 characters")
    private String reasonForVisit;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;
}
