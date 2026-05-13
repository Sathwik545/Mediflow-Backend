package com.mediflow.platform.patient.controller;

import com.mediflow.platform.common.response.ApiResponse;
import com.mediflow.platform.patient.dto.PatientCreationResponseDTO;
import com.mediflow.platform.patient.dto.PatientRequestDTO;
import com.mediflow.platform.patient.dto.PatientResponseDTO;
import com.mediflow.platform.patient.enums.PatientStatus;
import com.mediflow.platform.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "APIs for registering, retrieving, updating, and deactivating patients")
public class PatientController {

    private final PatientService patientService;

    @Operation(summary = "Register a new patient", description = "Creates a new patient record and generates a unique patient code")
    @PostMapping
    public ResponseEntity<ApiResponse<PatientCreationResponseDTO>> createPatient(
            @Valid @RequestBody PatientRequestDTO request) {

        PatientCreationResponseDTO response = patientService.createPatient(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    "Patient registered successfully. Share the temporary password securely with the patient.",
                    response));
    }

    @Operation(summary = "Get patient by code", description = "Retrieves full patient details using the unique patient code")
    @GetMapping("/{patientCode}")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> getPatientByCode(
            @PathVariable String patientCode) {

        PatientResponseDTO response = patientService.getPatientByCode(patientCode);
        return ResponseEntity.ok(ApiResponse.success("Patient retrieved successfully", response));
    }

    @Operation(summary = "Get all patients", description = "Returns a paginated list of patients. Filter by status (ACTIVE/INACTIVE) or full-text search via 'search' param (matches name, code, phone, email)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientResponseDTO>>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) PatientStatus status,
            @RequestParam(required = false) String search) {

        if (size > 100) size = 100;
        Page<PatientResponseDTO> patients = (search != null && !search.trim().isEmpty())
                ? patientService.searchPatients(search.trim(), page, size)
                : patientService.getAllPatients(page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Patients retrieved successfully", patients));
    }

    @Operation(summary = "Update patient", description = "Updates all fields of an existing patient record identified by patient code")
    @PutMapping("/{patientCode}")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> updatePatient(
            @PathVariable String patientCode,
            @Valid @RequestBody PatientRequestDTO request) {

        PatientResponseDTO response = patientService.updatePatient(patientCode, request);
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", response));
    }

    @Operation(summary = "Deactivate patient (soft delete)", description = "Sets patient status to INACTIVE. The record is preserved — medical data is never permanently deleted")
    @DeleteMapping("/{patientCode}")
    public ResponseEntity<ApiResponse<PatientResponseDTO>> deactivatePatient(@PathVariable String patientCode) {
        PatientResponseDTO response = patientService.deactivatePatient(patientCode);
        return ResponseEntity.ok(ApiResponse.success("Patient has been deactivated successfully", response));
    }
}
