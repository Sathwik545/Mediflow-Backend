package com.mediflow.platform.doctor.dto;

import com.mediflow.platform.doctor.enums.DoctorStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound DTO used for both creating and fully updating a doctor record (POST and PUT).
 *
 * Validation strategy:
 * - Format/constraint validations (blank, size, pattern, range) are declared here
 *   and enforced at the controller boundary via @Valid.
 * - Business-rule validations (email uniqueness, license uniqueness) are handled
 *   in DoctorServiceImpl to keep the DTO concern-free of DB access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequestDTO {

    // ── Personal Information ────────────────────────────────────────────────

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

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address (e.g., doctor@hospital.com)")
    @Size(max = 255, message = "Email address must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Phone number must be exactly 10 digits (numbers only, no spaces or special characters)"
    )
    private String phoneNumber;

    // ── Professional Information ────────────────────────────────────────────

    /**
     * Medical specialization (e.g., "Cardiology", "Orthopedics", "General Medicine").
     * Allows letters, spaces, commas, dots, slashes, and hyphens to cover
     * compound specializations like "Ortho/Spine" or "Internal Medicine, Critical Care".
     */
    @NotBlank(message = "Specialization is required")
    @Size(min = 2, max = 100, message = "Specialization must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s,./\\-]*$",
        message = "Specialization must start with a letter and may contain letters, spaces, commas, slashes, or hyphens"
    )
    private String specialization;

    /**
     * Academic qualifications (e.g., "MBBS", "MBBS, MD", "MS (Surgery)").
     * Allows letters, spaces, commas, dots, parentheses, and slashes to support
     * standard medical degree formats.
     */
    @NotBlank(message = "Qualification is required")
    @Size(min = 2, max = 150, message = "Qualification must be between 2 and 150 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s.,/()'\\-]*$",
        message = "Qualification must start with a letter and may contain letters, dots, commas, and parentheses (e.g., MBBS, M.D., MS (Surgery))"
    )
    private String qualification;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience cannot exceed 60 years")
    private Integer yearsOfExperience;

    /**
     * Consultation fee in the platform's base currency (INR).
     * Must be a positive value. BigDecimal is used to avoid floating-point precision issues.
     * Accepted format: up to 8 digits before decimal, 2 after (e.g., 500.00, 2500.50).
     */
    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "1.0", inclusive = true, message = "Consultation fee must be at least ₹1.00")
    @DecimalMax(value = "99999999.99", inclusive = true, message = "Consultation fee exceeds the maximum allowed value")
    @Digits(
        integer = 8, fraction = 2,
        message = "Consultation fee must have at most 8 digits before and 2 digits after the decimal point"
    )
    private BigDecimal consultationFee;

    /**
     * Official medical council license number (e.g., "MH-12345", "DL/2020/98765").
     * Must be uppercase alphanumeric and may include hyphens or slashes.
     * This is validated for uniqueness in the service layer.
     */
    @NotBlank(message = "License number is required")
    @Size(min = 5, max = 50, message = "License number must be between 5 and 50 characters")
    @Pattern(
        regexp = "^[A-Z0-9][A-Z0-9\\-/]{4,49}$",
        message = "License number must be uppercase and may only contain letters, digits, hyphens, or slashes (e.g., MH-12345)"
    )
    private String licenseNumber;

    /**
     * Hospital department (e.g., "Cardiology", "Emergency & Trauma", "Orthopaedics").
     * Allows letters, spaces, ampersands, commas, and hyphens for multi-word department names.
     */
    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z\\s&,\\-]*$",
        message = "Department name must start with a letter and may contain letters, spaces, ampersands, or hyphens"
    )
    private String department;

    /** Nullable — only used on PUT updates. Create always defaults to ACTIVE in the mapper. */
    private DoctorStatus status;
}
