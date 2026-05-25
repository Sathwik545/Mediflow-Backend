package com.mediflow.platform.lab.dto;

import com.mediflow.platform.lab.enums.LabOrderPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderRequestDTO {

    @NotBlank(message = "Consultation code is required")
    private String consultationCode;

    @NotNull(message = "Priority is required")
    private LabOrderPriority priority;

    private String clinicalNotes;
    private String instructions;

    @NotEmpty(message = "At least one test must be ordered")
    @Valid
    private List<LabOrderItemRequestDTO> items;
}
