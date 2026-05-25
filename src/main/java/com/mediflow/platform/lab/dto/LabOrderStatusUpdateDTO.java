package com.mediflow.platform.lab.dto;

import com.mediflow.platform.lab.enums.LabOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private LabOrderStatus status;
}
