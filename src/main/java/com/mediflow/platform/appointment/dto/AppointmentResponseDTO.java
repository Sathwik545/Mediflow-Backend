package com.mediflow.platform.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.enums.BookedBy;
import com.mediflow.platform.appointment.enums.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDTO {

    private String appointmentCode;

    private String patientCode;
    private String patientName;

    private String doctorCode;
    private String doctorName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;

    private ConsultationType consultationType;
    private AppointmentStatus appointmentStatus;
    private BookedBy bookedBy;

    private String reasonForVisit;
    private String notes;

    private BigDecimal consultationFee;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    // Audit identity — who created / last updated this record (email from JWT, never from payload)
    private String createdBy;
    private String updatedBy;
}
