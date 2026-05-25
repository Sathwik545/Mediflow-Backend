package com.mediflow.platform.billing.entity;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.billing.enums.BillStatus;
import com.mediflow.platform.billing.enums.BillType;
import com.mediflow.platform.billing.enums.PaymentMethod;
import com.mediflow.platform.billing.enums.PaymentStatus;
import com.mediflow.platform.common.audit.BaseAuditEntity;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for the 'bills' table — the financial record for a consultation appointment.
 *
 * consultationFeeSnapshot is frozen at bill generation time from Doctor.consultationFee,
 * ensuring historical financial records remain immutable regardless of future fee changes.
 *
 * Audit fields (createdAt, updatedAt, createdBy, updatedBy) are inherited from BaseAuditEntity
 * and populated automatically via Spring Data JPA auditing from the JWT SecurityContext.
 */
@Entity
@Table(
    name = "bills",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_bill_code",        columnNames = "bill_code"),
        @UniqueConstraint(name = "uq_bill_appointment", columnNames = "appointment_id")
    },
    indexes = {
        @Index(name = "idx_bill_code",       columnList = "bill_code"),
        @Index(name = "idx_bill_patient",    columnList = "patient_id"),
        @Index(name = "idx_bill_doctor",     columnList = "doctor_id"),
        @Index(name = "idx_bill_pay_status", columnList = "payment_status"),
        @Index(name = "idx_bill_status",     columnList = "bill_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_code", nullable = false, unique = true, length = 20)
    private String billCode;

    // One appointment produces exactly one consultation bill.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_type", nullable = false, length = 20)
    @Builder.Default
    private BillType billType = BillType.CONSULTATION;

    // Name snapshots — frozen at bill generation from appointment booking-time snapshots.
    // Historical invoices remain accurate even if patient or doctor names change later.
    @Column(name = "patient_name_snapshot", nullable = false, length = 255)
    private String patientNameSnapshot;

    @Column(name = "doctor_name_snapshot", nullable = false, length = 255)
    private String doctorNameSnapshot;

    // Frozen at generation time from Doctor.consultationFee — never recalculated dynamically.
    @Column(name = "consultation_fee_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFeeSnapshot;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_status", nullable = false, length = 20)
    @Builder.Default
    private BillStatus billStatus = BillStatus.GENERATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
