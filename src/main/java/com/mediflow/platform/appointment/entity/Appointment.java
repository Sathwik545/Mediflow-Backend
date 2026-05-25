package com.mediflow.platform.appointment.entity;

import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.enums.BookedBy;
import com.mediflow.platform.appointment.enums.ConsultationType;
import com.mediflow.platform.common.audit.BaseAuditEntity;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * JPA entity for the 'appointments' table — the transactional bridge between Patient and Doctor.
 *
 * Snapshot fields (doctorNameSnapshot, patientNameSnapshot, consultationFeeSnapshot) are frozen
 * at booking time to preserve historical accuracy regardless of future profile changes.
 *
 * Audit fields (createdAt, updatedAt, createdBy, updatedBy) are inherited from BaseAuditEntity
 * and populated automatically via Spring Data JPA auditing.
 * SecurityAuditorAware resolves createdBy / updatedBy from the authenticated user's email
 * in the JWT SecurityContext — never from client payloads.
 */
@Entity
@Table(
    name = "appointments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_appointment_code", columnNames = "appointment_code")
    },
    indexes = {
        @Index(name = "idx_apt_code",    columnList = "appointment_code"),
        @Index(name = "idx_apt_patient", columnList = "patient_id"),
        @Index(name = "idx_apt_doctor",  columnList = "doctor_id"),
        @Index(name = "idx_apt_date",    columnList = "appointment_date"),
        @Index(name = "idx_apt_status",  columnList = "appointment_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_code", nullable = false, unique = true, length = 20)
    private String appointmentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type", nullable = false, length = 20)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_status", nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus appointmentStatus = AppointmentStatus.PAYMENT_PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "booked_by", nullable = false, length = 20)
    private BookedBy bookedBy;

    @Column(name = "reason_for_visit", length = 1000)
    private String reasonForVisit;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Snapshot fields — frozen at booking time to preserve historical accuracy.
    // These ensure old records remain accurate even when doctor/patient details change later.
    @Column(name = "doctor_name_snapshot", nullable = false, length = 255)
    private String doctorNameSnapshot;

    @Column(name = "patient_name_snapshot", nullable = false, length = 255)
    private String patientNameSnapshot;

    @Column(name = "consultation_fee_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFeeSnapshot;

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
