package com.mediflow.platform.lab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReportResponseDTO {

    private String reportCode;
    private String reportStatus;

    private String labOrderCode;
    private Long labOrderItemId;
    private String testCode;
    private String testName;
    private String testCategory;

    private String patientCode;
    private String patientName;
    private String doctorCode;
    private String doctorName;

    private String resultValue;
    private String referenceRange;
    private Boolean abnormalFlag;
    private String interpretation;
    private String remarks;

    private List<ReportAttachmentResponseDTO> attachments;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;
}
