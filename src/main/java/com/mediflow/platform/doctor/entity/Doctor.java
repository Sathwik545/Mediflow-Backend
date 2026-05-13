package com.mediflow.platform.doctor.entity;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.auth.entity.User;
import com.mediflow.platform.common.audit.BaseAuditEntity;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a registered doctor in the MediFlow platform.
 *
 * Design notes:
 * - doctorCode (format: DOC-YYYY-NNNN) is the public-facing business identifier.
 *   The internal 'id' must never be exposed in API responses or URL paths.
 * - licenseNumber is the official medical council registration — unique per practitioner.
 * - consultationFee uses BigDecimal (DECIMAL precision in DB) to prevent floating-point
 *   rounding errors that occur with double/float for monetary values.
 * - status supports soft delete (INACTIVE) and temporary absence (ON_LEAVE) to preserve
 *   referential integrity with appointments and other related records.
 * - Audit fields (createdAt, updatedAt, createdBy, updatedBy) are inherited from
 *   BaseAuditEntity and populated automatically via Spring Data JPA auditing.
 *   SecurityAuditorAware resolves createdBy / updatedBy from the authenticated
 *   user's email in the JWT SecurityContext — never from client payloads.
 */
@Entity
@Table(
    name = "doctors",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_doctor_code",    columnNames = "doctor_code"),
        @UniqueConstraint(name = "uq_doctor_email",   columnNames = "email"),
        @UniqueConstraint(name = "uq_doctor_license", columnNames = "license_number")
    },
    indexes = {
        @Index(name = "idx_doctor_code",           columnList = "doctor_code"),
        @Index(name = "idx_doctor_email",          columnList = "email"),
        @Index(name = "idx_doctor_status",         columnList = "status"),
        @Index(name = "idx_doctor_department",     columnList = "department"),
        @Index(name = "idx_doctor_specialization", columnList = "specialization")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Business-facing unique identifier. Format: DOC-YYYY-NNNN (e.g., DOC-2026-0001). */
    @Column(name = "doctor_code", nullable = false, unique = true, length = 15)
    private String doctorCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /** Medical specialization, e.g., Cardiology, Neurology, Pediatrics. */
    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;

    /** Academic qualifications, e.g., "MBBS, MD" or "MS (Surgery)". */
    @Column(name = "qualification", nullable = false, length = 150)
    private String qualification;

    @Column(name = "years_of_experience", nullable = false)
    private Integer yearsOfExperience;

    /**
     * Consultation fee stored as DECIMAL(10,2) to ensure monetary precision.
     * Using BigDecimal on the Java side avoids IEEE 754 floating-point issues.
     */
    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    /** Official medical council license number — unique per practitioner. */
    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private DoctorStatus status = DoctorStatus.ACTIVE;

    /**
     * Link to the centralized User identity (authentication account).
     * Nullable for backward compatibility with doctor records created before auth module deployment.
     * All new doctors created via the API will always have a linked User account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
