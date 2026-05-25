package com.mediflow.platform.consultation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.platform.appointment.enums.ConsultationType;
import com.mediflow.platform.consultation.enums.ConsultationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class ConsultationResponseDTO {

    private Long id;
    private String consultationCode;

    // ── Appointment context (readonly clinical reference) ─────────────────────────

    private String appointmentCode;
    private ConsultationType consultationType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    // ── Patient & Doctor (derived from appointment, readonly) ─────────────────────

    private String patientCode;
    private String patientName;
    private String patientGender;
    private Integer patientAge;

    private String doctorCode;
    private String doctorName;
    private String doctorSpecialization;
    private String doctorDepartment;

    // ── Vitals ────────────────────────────────────────────────────────────────────

    private String bloodPressure;
    private Integer pulseRate;
    private BigDecimal temperature;
    private Integer oxygenSaturation;
    private Integer respiratoryRate;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bmi;

    // ── Clinical ──────────────────────────────────────────────────────────────────

    private String chiefComplaint;
    private String symptoms;
    private String diagnosis;
    private String doctorNotes;
    private String treatmentPlan;

    // ── Follow-up ─────────────────────────────────────────────────────────────────

    private Boolean followUpRequired;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate followUpDate;

    private String followUpNotes;

    // ── Status ────────────────────────────────────────────────────────────────────

    private ConsultationStatus consultationStatus;

    // ── Prescriptions ─────────────────────────────────────────────────────────────

    private List<PrescriptionItemResponseDTO> prescriptionItems;

    // ── Audit ─────────────────────────────────────────────────────────────────────

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;
}
