package com.mediflow.platform.patient.entity;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.auth.entity.User;
import com.mediflow.platform.common.audit.BaseAuditEntity;
import com.mediflow.platform.patient.enums.BloodGroup;
import com.mediflow.platform.patient.enums.Gender;
import com.mediflow.platform.patient.enums.PatientStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the 'patients' table.
 *
 * Audit fields (createdAt, updatedAt, createdBy, updatedBy) are inherited from
 * BaseAuditEntity and populated automatically via Spring Data JPA auditing.
 * SecurityAuditorAware resolves createdBy / updatedBy from the authenticated
 * user's email in the JWT SecurityContext — never from client payloads.
 */
@Entity
@Table(
    name = "patients",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_patient_code", columnNames = "patient_code"),
        @UniqueConstraint(name = "uq_patient_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_patient_code", columnList = "patient_code"),
        @Index(name = "idx_patient_email", columnList = "email"),
        @Index(name = "idx_patient_status", columnList = "status"),
        @Index(name = "idx_patient_last_name", columnList = "last_name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_code", nullable = false, unique = true, length = 20)
    private String patientCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", length = 15)
    private BloodGroup bloodGroup;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(name = "emergency_contact_name", nullable = false, length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", nullable = false, length = 15)
    private String emergencyContactPhone;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private PatientStatus status = PatientStatus.ACTIVE;

    /**
     * Link to the centralized User identity (authentication account).
     * Nullable for backward compatibility with patient records created before auth module deployment.
     * All new patients created via the API will always have a linked User account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
