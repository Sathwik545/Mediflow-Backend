package com.mediflow.platform.patient.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.platform.patient.enums.BloodGroup;
import com.mediflow.platform.patient.enums.Gender;
import com.mediflow.platform.patient.enums.PatientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDTO {

    private String patientCode;
    private String firstName;
    private String lastName;
    private String fullName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private Gender gender;
    private BloodGroup bloodGroup;
    private String phoneNumber;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String allergies;
    private String medicalHistory;
    private PatientStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    // Audit identity — who created / last updated this record (email from JWT, never from payload)
    private String createdBy;
    private String updatedBy;
}
