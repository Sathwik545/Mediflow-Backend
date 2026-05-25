package com.mediflow.platform.lab.mapper;

import com.mediflow.platform.lab.dto.LabReportResponseDTO;
import com.mediflow.platform.lab.dto.ReportAttachmentResponseDTO;
import com.mediflow.platform.lab.entity.LabReport;
import com.mediflow.platform.lab.entity.ReportAttachment;

import java.util.List;

public class LabReportMapper {

    public static LabReportResponseDTO toResponseDTO(LabReport report) {
        List<ReportAttachmentResponseDTO> attachmentDTOs = report.getAttachments().stream()
                .map(LabReportMapper::toAttachmentDTO)
                .toList();

        return LabReportResponseDTO.builder()
                .reportCode(report.getReportCode())
                .reportStatus(report.getReportStatus().name())
                .labOrderCode(report.getLabOrder().getLabOrderCode())
                .labOrderItemId(report.getLabOrderItem().getId())
                .testCode(report.getLabOrderItem().getTestCode())
                .testName(report.getLabOrderItem().getTestName())
                .testCategory(report.getLabOrderItem().getCategory())
                .patientCode(report.getPatient().getPatientCode())
                .patientName(report.getPatient().getFirstName() + " " + report.getPatient().getLastName())
                .doctorCode(report.getDoctor().getDoctorCode())
                .doctorName(report.getDoctor().getFirstName() + " " + report.getDoctor().getLastName())
                .resultValue(report.getResultValue())
                .referenceRange(report.getReferenceRange())
                .abnormalFlag(report.getAbnormalFlag())
                .interpretation(report.getInterpretation())
                .remarks(report.getRemarks())
                .attachments(attachmentDTOs)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .createdBy(report.getCreatedBy())
                .updatedBy(report.getUpdatedBy())
                .build();
    }

    public static ReportAttachmentResponseDTO toAttachmentDTO(ReportAttachment attachment) {
        return ReportAttachmentResponseDTO.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .fileUrl(attachment.getFileUrl())
                .uploadedAt(attachment.getCreatedAt())
                .uploadedBy(attachment.getCreatedBy())
                .build();
    }
}
