package com.mediflow.platform.doctor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outbound DTO returned in API responses for doctor-related operations.
 *
 * Note: The internal database 'id' is intentionally excluded.
 * All cross-service references and URL paths should use 'doctorCode'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponseDTO {

    private String doctorCode;
    private String firstName;
    private String lastName;
    private String fullName;            // Computed: firstName + " " + lastName
    private String email;
    private String phoneNumber;
    private String specialization;
    private String qualification;
    private Integer yearsOfExperience;
    private BigDecimal consultationFee;
    private String licenseNumber;
    private String department;
    private DoctorStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    // Audit identity — who created / last updated this record (email from JWT, never from payload)
    private String createdBy;
    private String updatedBy;
}
