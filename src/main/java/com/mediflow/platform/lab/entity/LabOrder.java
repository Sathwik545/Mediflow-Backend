package com.mediflow.platform.lab.entity;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.common.audit.BaseAuditEntity;
import com.mediflow.platform.consultation.entity.Consultation;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.lab.enums.LabOrderPriority;
import com.mediflow.platform.lab.enums.LabOrderStatus;
import com.mediflow.platform.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "lab_orders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_lab_order_code", columnNames = "lab_order_code")
    },
    indexes = {
        @Index(name = "idx_lab_order_code",         columnList = "lab_order_code"),
        @Index(name = "idx_lab_order_patient",       columnList = "patient_id"),
        @Index(name = "idx_lab_order_doctor",        columnList = "doctor_id"),
        @Index(name = "idx_lab_order_consultation",  columnList = "consultation_id"),
        @Index(name = "idx_lab_order_status",        columnList = "lab_order_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrder extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_order_code", nullable = false, unique = true, length = 20)
    private String labOrderCode;

    // ── Relationships ───────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // ── Order Metadata ──────────────────────────────────────────────────────────

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "lab_order_priority", nullable = false, length = 20)
    @Builder.Default
    private LabOrderPriority priority = LabOrderPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "lab_order_status", nullable = false, length = 25)
    @Builder.Default
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    // ── Test Items ──────────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy = "labOrder",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<LabOrderItem> items = new ArrayList<>();

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
