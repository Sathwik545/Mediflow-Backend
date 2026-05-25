package com.mediflow.platform.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderItemResponseDTO {
    private Long id;
    private String testCode;
    private String testName;
    private String category;
    private String remarks;
    private boolean hasReport;
    private String reportCode;
    private String reportStatus;
}
