package com.mediflow.platform.patient.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.platform.patient.enums.BloodGroup;
import com.mediflow.platform.patient.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s'\\-.]*$",
        message = "First name must start with a letter and may only contain letters, spaces, hyphens, or apostrophes"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s'\\-.]*$",
        message = "Last name must start with a letter and may only contain letters, spaces, hyphens, or apostrophes"
    )
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a date in the past")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private BloodGroup bloodGroup;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Phone number must be exactly 10 digits (numbers only, no spaces or special characters)"
    )
    private String phoneNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address (e.g., john.doe@example.com)")
    @Size(max = 255, message = "Email address must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Address line 1 is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s'\\-.]*$",
        message = "City name must start with a letter and may only contain letters and spaces"
    )
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100, message = "State name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s'\\-.]*$",
        message = "State name must start with a letter and may only contain letters and spaces"
    )
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(
        regexp = "^[0-9]{6}$",
        message = "Postal code must be exactly 6 digits"
    )
    private String postalCode;

    @NotBlank(message = "Emergency contact name is required")
    @Size(min = 2, max = 100, message = "Emergency contact name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s'\\-.]*$",
        message = "Emergency contact name must start with a letter and may only contain letters, spaces, hyphens, or apostrophes"
    )
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Emergency contact phone must be exactly 10 digits (numbers only, no spaces or special characters)"
    )
    private String emergencyContactPhone;

    @Size(max = 2000, message = "Allergies description must not exceed 2000 characters")
    private String allergies;

    @Size(max = 5000, message = "Medical history must not exceed 5000 characters")
    private String medicalHistory;
}
