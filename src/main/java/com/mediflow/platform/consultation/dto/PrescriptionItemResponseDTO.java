package com.mediflow.platform.consultation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrescriptionItemResponseDTO {

    private Long id;
    private String medicineName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
}
