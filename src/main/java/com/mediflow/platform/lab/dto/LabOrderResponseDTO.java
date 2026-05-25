package com.mediflow.platform.lab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderResponseDTO {

    private String labOrderCode;
    private LocalDate orderDate;
    private String priority;
    private String status;

    // Derived from consultation → appointment linkage (never from payload)
    private String consultationCode;
    private String appointmentCode;
    private String patientCode;
    private String patientName;
    private String doctorCode;
    private String doctorName;

    private String clinicalNotes;
    private String instructions;

    private List<LabOrderItemResponseDTO> items;
    private int testsCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;
}
