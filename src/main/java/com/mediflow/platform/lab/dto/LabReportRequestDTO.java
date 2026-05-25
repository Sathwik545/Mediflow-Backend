package com.mediflow.platform.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReportRequestDTO {

    @NotBlank(message = "Lab order code is required")
    private String labOrderCode;

    @NotNull(message = "Lab order item ID is required")
    private Long labOrderItemId;

    private String resultValue;
    private String referenceRange;
    private Boolean abnormalFlag;
    private String interpretation;
    private String remarks;
}
