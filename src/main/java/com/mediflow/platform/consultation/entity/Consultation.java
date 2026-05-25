package com.mediflow.platform.consultation.entity;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.common.audit.BaseAuditEntity;
import com.mediflow.platform.consultation.entity.PrescriptionItem;
import com.mediflow.platform.consultation.enums.ConsultationStatus;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "consultations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_consultation_code",       columnNames = "consultation_code"),
        @UniqueConstraint(name = "uq_consultation_appointment", columnNames = "appointment_id")
    },
    indexes = {
        @Index(name = "idx_cons_code",       columnList = "consultation_code"),
        @Index(name = "idx_cons_patient",    columnList = "patient_id"),
        @Index(name = "idx_cons_doctor",     columnList = "doctor_id"),
        @Index(name = "idx_cons_status",     columnList = "consultation_status"),
        @Index(name = "idx_cons_appointment",columnList = "appointment_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_code", nullable = false, unique = true, length = 20)
    private String consultationCode;

    // ── Relationships ───────────────────────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // ── Snapshots (frozen at consultation creation time) ────────────────────────

    @Column(name = "patient_name_snapshot", length = 255)
    private String patientNameSnapshot;

    @Column(name = "doctor_name_snapshot", length = 255)
    private String doctorNameSnapshot;

    // ── Vitals ──────────────────────────────────────────────────────────────────

    /** Free-text blood pressure, e.g. "120/80" or "120/80 mmHg" */
    @Column(name = "blood_pressure", length = 30)
    private String bloodPressure;

    /** Pulse rate in beats per minute */
    @Column(name = "pulse_rate")
    private Integer pulseRate;

    /** Body temperature in °C */
    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    /** SpO2 oxygen saturation percentage (0–100) */
    @Column(name = "oxygen_saturation")
    private Integer oxygenSaturation;

    /** Respiratory rate in breaths per minute */
    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    /** Height in centimetres */
    @Column(name = "height", precision = 6, scale = 2)
    private BigDecimal height;

    /** Weight in kilograms */
    @Column(name = "weight", precision = 6, scale = 2)
    private BigDecimal weight;

    /** Body Mass Index — can be supplied by the doctor or auto-calculated from height + weight */
    @Column(name = "bmi", precision = 5, scale = 2)
    private BigDecimal bmi;

    // ── Clinical Details ─────────────────────────────────────────────────────────

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    // ── Follow-up ────────────────────────────────────────────────────────────────

    @Column(name = "follow_up_required")
    @Builder.Default
    private Boolean followUpRequired = false;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    // ── Status ───────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_status", nullable = false, length = 20)
    @Builder.Default
    private ConsultationStatus consultationStatus = ConsultationStatus.DRAFT;

    // ── Prescription Items ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy = "consultation",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PrescriptionItem> prescriptionItems = new ArrayList<>();

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
