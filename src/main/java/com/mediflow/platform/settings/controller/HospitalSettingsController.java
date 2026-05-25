package com.mediflow.platform.settings.controller;

import com.mediflow.platform.common.response.ApiResponse;
import com.mediflow.platform.settings.dto.HospitalSettingsRequestDTO;
import com.mediflow.platform.settings.dto.HospitalSettingsResponseDTO;
import com.mediflow.platform.settings.service.HospitalSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for hospital / organization configuration.
 *
 * Base path: /api/v1/settings/hospital
 *
 * Intentionally exposes only two operations:
 *   GET  — read current settings (ADMIN only)
 *   PUT  — update settings      (ADMIN only)
 *
 * No POST (create) and no DELETE are exposed — the single-row invariant
 * is enforced entirely at the service and DataInitializer layer.
 */
@RestController
@RequestMapping("/api/v1/settings/hospital")
@RequiredArgsConstructor
@Tag(name = "Hospital Settings", description = "Organization configuration APIs — single-record settings used as the source of truth for invoices, branding, and operational data")
public class HospitalSettingsController {

    private final HospitalSettingsService hospitalSettingsService;

    @Operation(
        summary = "Get hospital settings",
        description = "Returns the current organization configuration for MediFlow. " +
                      "This record is the authoritative source for invoice generation, branding, " +
                      "and contact information. Restricted to ADMIN role."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HospitalSettingsResponseDTO>> getSettings() {
        HospitalSettingsResponseDTO settings = hospitalSettingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("Hospital settings retrieved successfully", settings));
    }

    @Operation(
        summary = "Update hospital settings",
        description = "Updates the organization configuration. If the settings record does not yet exist " +
                      "(e.g., DataInitializer was skipped), it is created. Only one settings record ever " +
                      "exists — this endpoint never creates a second record. " +
                      "Required: hospitalName, hospitalCode, currencyCode, timezone. " +
                      "Restricted to ADMIN role."
    )
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HospitalSettingsResponseDTO>> updateSettings(
            @Valid @RequestBody HospitalSettingsRequestDTO request) {

        HospitalSettingsResponseDTO settings = hospitalSettingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success("Hospital settings updated successfully", settings));
    }
}
