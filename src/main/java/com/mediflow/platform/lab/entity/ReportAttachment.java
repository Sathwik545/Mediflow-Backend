package com.mediflow.platform.lab.entity;

import com.mediflow.platform.common.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "report_attachments",
    indexes = {
        @Index(name = "idx_report_attachment_report", columnList = "lab_report_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportAttachment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_report_id", nullable = false)
    private LabReport labReport;

    /** Server-side unique filename: {UUID}_{sanitized-original}. Never the raw upload name. */
    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    /** Exact filename supplied by the client at upload time. */
    @Column(name = "original_file_name", nullable = false, length = 300)
    private String originalFileName;

    /** MIME type (application/pdf, image/png, image/jpeg). */
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    /** File size in bytes. */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** Relative path from the application root: uploads/reports/{fileName}. */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    // uploadedAt / uploadedBy are provided by BaseAuditEntity createdAt / createdBy
    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
