package com.mediflow.platform.lab.entity;

import com.mediflow.platform.common.audit.BaseAuditEntity;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.lab.enums.ReportStatus;
import com.mediflow.platform.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "lab_reports",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_lab_report_code",             columnNames = "report_code"),
        @UniqueConstraint(name = "uq_lab_report_order_item",       columnNames = {"lab_order_id", "lab_order_item_id"})
    },
    indexes = {
        @Index(name = "idx_lab_report_code",      columnList = "report_code"),
        @Index(name = "idx_lab_report_order",     columnList = "lab_order_id"),
        @Index(name = "idx_lab_report_item",      columnList = "lab_order_item_id"),
        @Index(name = "idx_lab_report_patient",   columnList = "patient_id"),
        @Index(name = "idx_lab_report_doctor",    columnList = "doctor_id"),
        @Index(name = "idx_lab_report_status",    columnList = "report_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReport extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_code", nullable = false, unique = true, length = 20)
    private String reportCode;

    // ── Relationships ───────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_item_id", nullable = false)
    private LabOrderItem labOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // ── Result Data ─────────────────────────────────────────────────────────────

    @Column(name = "result_value", columnDefinition = "TEXT")
    private String resultValue;

    @Column(name = "reference_range", length = 200)
    private String referenceRange;

    @Column(name = "abnormal_flag")
    @Builder.Default
    private Boolean abnormalFlag = false;

    @Column(name = "interpretation", columnDefinition = "TEXT")
    private String interpretation;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // ── Historical Snapshots ──────────────────────────────────────────────────────
    // Frozen at report creation time so generated PDFs remain historically accurate
    // even if patient/doctor names are later updated. Nullable for backward compatibility
    // with records created before this feature was added; PDF service falls back to live data.

    @Column(name = "patient_name_snapshot", length = 255)
    private String patientNameSnapshot;

    @Column(name = "doctor_name_snapshot", length = 255)
    private String doctorNameSnapshot;

    // ── Status ───────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus reportStatus = ReportStatus.PENDING;

    // ── Attachments ───────────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy = "labReport",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ReportAttachment> attachments = new ArrayList<>();

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
