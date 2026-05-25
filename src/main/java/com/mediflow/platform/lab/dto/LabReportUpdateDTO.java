package com.mediflow.platform.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReportUpdateDTO {
    private String resultValue;
    private String referenceRange;
    private Boolean abnormalFlag;
    private String interpretation;
    private String remarks;
}
