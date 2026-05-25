package com.mediflow.platform.settings.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for updating hospital / organization settings.
 *
 * Required fields: hospitalName, hospitalCode, currencyCode, timezone.
 * All other fields are optional — null values clear the stored value.
 *
 * Validation notes:
 *  - @Pattern annotations are skipped for null values (Jakarta Bean Validation spec).
 *  - Empty strings sent by the frontend are converted to null in the service layer
 *    before persisting so the DB stores clean nulls rather than empty strings.
 *  - Audit fields (createdBy, updatedBy, etc.) are NEVER accepted here — they
 *    come exclusively from the JWT SecurityContext via Spring Data JPA auditing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalSettingsRequestDTO {

    // ── Identity (required) ───────────────────────────────────────────────────

    @NotBlank(message = "Hospital name is required")
    @Size(max = 255, message = "Hospital name must not exceed 255 characters")
    private String hospitalName;

    @NotBlank(message = "Hospital code is required")
    @Size(max = 50, message = "Hospital code must not exceed 50 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9_\\-]*$",
        message = "Hospital code must start with a letter or digit and contain only letters, digits, hyphens, or underscores"
    )
    private String hospitalCode;

    // ── Contact (optional) ────────────────────────────────────────────────────

    @Pattern(
        regexp = "^\\+?[0-9][0-9\\s\\-()]{5,18}$",
        message = "Phone number must be 7–20 characters: digits, spaces, hyphens, or parentheses"
    )
    private String phoneNumber;

    @Pattern(
        regexp = "^\\+?[0-9][0-9\\s\\-()]{5,18}$",
        message = "Alternate phone number must be 7–20 characters: digits, spaces, hyphens, or parentheses"
    )
    private String alternatePhoneNumber;

    @Email(message = "Invalid email address format")
    @Size(max = 255)
    private String email;

    @Email(message = "Invalid support email address format")
    @Size(max = 255)
    private String supportEmail;

    @Size(max = 255, message = "Website URL must not exceed 255 characters")
    private String website;

    @Pattern(
        regexp = "^\\+?[0-9][0-9\\s\\-()]{5,18}$",
        message = "Support phone must be 7–20 characters: digits, spaces, hyphens, or parentheses"
    )
    private String supportPhone;

    // ── Address (optional) ────────────────────────────────────────────────────

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(min = 3, max = 10, message = "Postal code must be between 3 and 10 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    // ── Branding (optional) ───────────────────────────────────────────────────

    @Size(max = 1000, message = "Logo URL must not exceed 1000 characters")
    private String logoUrl;

    // ── Financial / Legal ─────────────────────────────────────────────────────

    @Pattern(
        regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
        message = "Invalid GST number format — expected: 22AAAAA0000A1Z5 (15 uppercase characters)"
    )
    private String gstNumber;

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 5, message = "Currency code must be 3–5 characters (e.g., INR, USD)")
    private String currencyCode;

    @NotBlank(message = "Timezone is required")
    @Size(max = 60, message = "Timezone must not exceed 60 characters")
    private String timezone;
}
