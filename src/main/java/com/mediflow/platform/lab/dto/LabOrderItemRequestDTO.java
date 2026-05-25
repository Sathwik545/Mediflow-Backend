package com.mediflow.platform.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderItemRequestDTO {

    @NotBlank(message = "Test name is required")
    @Size(max = 200, message = "Test name must not exceed 200 characters")
    private String testName;

    @Size(max = 50, message = "Test code must not exceed 50 characters")
    private String testCode;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    private String remarks;
}
