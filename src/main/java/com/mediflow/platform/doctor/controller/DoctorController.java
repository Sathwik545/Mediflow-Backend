package com.mediflow.platform.doctor.controller;

import com.mediflow.platform.common.response.ApiResponse;
import com.mediflow.platform.doctor.dto.DoctorCreationResponseDTO;
import com.mediflow.platform.doctor.dto.DoctorRequestDTO;
import com.mediflow.platform.doctor.dto.DoctorResponseDTO;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import com.mediflow.platform.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Management", description = "APIs for registering, retrieving, updating, and deactivating doctors")
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(
        summary = "Register a new doctor",
        description = "Creates a new doctor record. Assigns a unique doctor code (DOC-YYYY-NNNN) automatically."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<DoctorCreationResponseDTO>> createDoctor(
            @Valid @RequestBody DoctorRequestDTO request) {

        DoctorCreationResponseDTO response = doctorService.createDoctor(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    "Doctor registered successfully. Share the temporary password securely with the doctor.",
                    response));
    }

    @Operation(
        summary = "Get doctor by code",
        description = "Retrieves full details of a doctor using the unique doctor code (e.g., DOC-2026-0001)."
    )
    @GetMapping("/{doctorCode}")
    public ResponseEntity<ApiResponse<DoctorResponseDTO>> getDoctorByCode(
            @PathVariable String doctorCode) {

        DoctorResponseDTO response = doctorService.getDoctorByCode(doctorCode);
        return ResponseEntity.ok(ApiResponse.success("Doctor retrieved successfully", response));
    }

    @Operation(
        summary = "Get all doctors",
        description = "Returns a paginated list of doctors. " +
                      "Optional 'status' filter accepts: ACTIVE, ON_LEAVE, INACTIVE. " +
                      "Optional 'search' param matches name, specialization, or doctor code (ACTIVE only). " +
                      "Results are sorted by registration date (newest first)."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DoctorResponseDTO>>> getAllDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) DoctorStatus status,
            @RequestParam(required = false) String search) {

        if (size > 100) size = 100;
        Page<DoctorResponseDTO> doctors = (search != null && !search.trim().isEmpty())
                ? doctorService.searchDoctors(search.trim(), page, size)
                : doctorService.getAllDoctors(page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved successfully", doctors));
    }

    @Operation(
        summary = "Update doctor",
        description = "Fully replaces all mutable fields of an existing doctor. " +
                      "The doctorCode is immutable and cannot be changed via this endpoint."
    )
    @PutMapping("/{doctorCode}")
    public ResponseEntity<ApiResponse<DoctorResponseDTO>> updateDoctor(
            @PathVariable String doctorCode,
            @Valid @RequestBody DoctorRequestDTO request) {

        DoctorResponseDTO response = doctorService.updateDoctor(doctorCode, request);
        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully", response));
    }

    @Operation(
        summary = "Deactivate doctor (soft delete)",
        description = "Sets the doctor's status to INACTIVE. " +
                      "Records are never permanently deleted — they are preserved for audit " +
                      "trails and referential integrity with appointment history."
    )
    @DeleteMapping("/{doctorCode}")
    public ResponseEntity<ApiResponse<DoctorResponseDTO>> deactivateDoctor(@PathVariable String doctorCode) {
        DoctorResponseDTO response = doctorService.deactivateDoctor(doctorCode);
        return ResponseEntity.ok(ApiResponse.success("Doctor has been deactivated successfully", response));
    }
}
