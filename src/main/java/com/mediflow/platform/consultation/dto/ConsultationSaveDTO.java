package com.mediflow.platform.consultation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for both DRAFT save and COMPLETE operations.
 *
 * For DRAFT: all clinical fields are optional — doctors may save partial work.
 * For COMPLETE: chiefComplaint and diagnosis become required (validated in service layer).
 *
 * Vital-sign range validations are applied regardless of draft vs. complete:
 * - pulseRate must be positive (>0)
 * - oxygenSaturation must be 0–100
 * - respiratoryRate must be positive
 * - weight and height must be positive
 * - temperature must be in a clinically plausible range (30–45 °C)
 *
 * Null values skip validation for all optional fields (JSR-380 default behaviour for boxed types).
 */
@Getter
@Setter
@NoArgsConstructor
public class ConsultationSaveDTO {

    // ── Vitals ───────────────────────────────────────────────────────────────────

    @Size(max = 30, message = "Blood pressure value must not exceed 30 characters")
    private String bloodPressure;

    @Min(value = 1,   message = "Pulse rate must be greater than 0")
    @Max(value = 300, message = "Pulse rate must not exceed 300 bpm")
    private Integer pulseRate;

    @DecimalMin(value = "30.0", message = "Temperature must be at least 30.0 °C")
    @DecimalMax(value = "45.0", message = "Temperature must not exceed 45.0 °C")
    private BigDecimal temperature;

    @Min(value = 0,   message = "Oxygen saturation must be at least 0")
    @Max(value = 100, message = "Oxygen saturation must not exceed 100")
    private Integer oxygenSaturation;

    @Min(value = 1,  message = "Respiratory rate must be greater than 0")
    @Max(value = 100, message = "Respiratory rate must not exceed 100")
    private Integer respiratoryRate;

    @DecimalMin(value = "0.1", message = "Height must be greater than 0")
    private BigDecimal height;

    @DecimalMin(value = "0.1", message = "Weight must be greater than 0")
    private BigDecimal weight;

    @DecimalMin(value = "0.1", message = "BMI must be greater than 0")
    private BigDecimal bmi;

    // ── Clinical ─────────────────────────────────────────────────────────────────

    @Size(max = 2000, message = "Chief complaint must not exceed 2000 characters")
    private String chiefComplaint;

    @Size(max = 5000, message = "Symptoms must not exceed 5000 characters")
    private String symptoms;

    @Size(max = 5000, message = "Diagnosis must not exceed 5000 characters")
    private String diagnosis;

    @Size(max = 10000, message = "Doctor notes must not exceed 10000 characters")
    private String doctorNotes;

    @Size(max = 5000, message = "Treatment plan must not exceed 5000 characters")
    private String treatmentPlan;

    // ── Follow-up ────────────────────────────────────────────────────────────────

    private Boolean followUpRequired;

    @Future(message = "Follow-up date must be a future date")
    private LocalDate followUpDate;

    @Size(max = 2000, message = "Follow-up notes must not exceed 2000 characters")
    private String followUpNotes;

    // ── Prescriptions ────────────────────────────────────────────────────────────

    @Valid
    private List<PrescriptionItemRequestDTO> prescriptionItems;
}
