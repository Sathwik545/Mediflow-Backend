package com.mediflow.platform.appointment.mapper;

import com.mediflow.platform.appointment.dto.AppointmentRequestDTO;
import com.mediflow.platform.appointment.dto.AppointmentResponseDTO;
import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.patient.entity.Patient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppointmentMapper {

    /**
     * Creates a new Appointment entity from the booking request.
     * Snapshot fields are populated from the live Patient and Doctor entities at booking time
     * so that historical records remain accurate even if names or fees change later.
     */
    public static Appointment toEntity(
            AppointmentRequestDTO dto,
            Patient patient,
            Doctor doctor,
            String appointmentCode) {

        String patientFullName = patient.getFirstName() + " " + patient.getLastName();
        String doctorFullName  = doctor.getFirstName()  + " " + doctor.getLastName();

        return Appointment.builder()
                .appointmentCode(appointmentCode)
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(dto.getAppointmentDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .consultationType(dto.getConsultationType())
                .appointmentStatus(AppointmentStatus.PAYMENT_PENDING)
                .bookedBy(dto.getBookedBy())
                .reasonForVisit(dto.getReasonForVisit() != null ? dto.getReasonForVisit().trim() : null)
                .notes(dto.getNotes() != null ? dto.getNotes().trim() : null)
                .patientNameSnapshot(patientFullName)
                .doctorNameSnapshot(doctorFullName)
                .consultationFeeSnapshot(doctor.getConsultationFee())
                .build();
    }

    /**
     * Converts an Appointment entity to the outbound response DTO.
     * Patient and doctor names are served from snapshot fields, not from the live entities,
     * to maintain historical consistency.
     */
    public static AppointmentResponseDTO toResponseDTO(Appointment appointment) {
        return AppointmentResponseDTO.builder()
                .appointmentCode(appointment.getAppointmentCode())
                .patientCode(appointment.getPatient().getPatientCode())
                .patientName(appointment.getPatientNameSnapshot())
                .doctorCode(appointment.getDoctor().getDoctorCode())
                .doctorName(appointment.getDoctorNameSnapshot())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .consultationType(appointment.getConsultationType())
                .appointmentStatus(appointment.getAppointmentStatus())
                .bookedBy(appointment.getBookedBy())
                .reasonForVisit(appointment.getReasonForVisit())
                .notes(appointment.getNotes())
                .consultationFee(appointment.getConsultationFeeSnapshot())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .createdBy(appointment.getCreatedBy())
                .updatedBy(appointment.getUpdatedBy())
                .build();
    }
}
