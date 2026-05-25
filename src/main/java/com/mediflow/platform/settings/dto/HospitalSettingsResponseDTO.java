package com.mediflow.platform.settings.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for hospital / organization settings.
 *
 * Consumed by:
 *  - Frontend Settings → Hospital Settings page
 *  - Future: Invoice / Receipt PDF generation service
 *  - Future: Notification and email templates
 *  - Future: Report headers and export branding
 *
 * All fields are nullable — the invoice module must handle null values gracefully
 * and fall back to empty strings rather than throwing NullPointerExceptions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalSettingsResponseDTO {

    private Long id;

    // ── Identity ──────────────────────────────────────────────────────────────

    private String hospitalCode;
    private String hospitalName;

    // ── Contact ───────────────────────────────────────────────────────────────

    private String phoneNumber;
    private String alternatePhoneNumber;
    private String email;
    private String supportEmail;
    private String website;
    private String supportPhone;

    // ── Address ───────────────────────────────────────────────────────────────

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // ── Branding ──────────────────────────────────────────────────────────────

    private String logoUrl;

    // ── Financial / Legal ─────────────────────────────────────────────────────

    private String gstNumber;
    private String currencyCode;
    private String timezone;

    // ── Audit (read-only, set by server) ─────────────────────────────────────

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;
}
