package com.mediflow.platform.consultation.controller;

import com.mediflow.platform.common.response.ApiResponse;
import com.mediflow.platform.consultation.dto.ConsultationResponseDTO;
import com.mediflow.platform.consultation.dto.ConsultationSaveDTO;
import com.mediflow.platform.consultation.enums.ConsultationStatus;
import com.mediflow.platform.consultation.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consultations")
@RequiredArgsConstructor
@Tag(name = "Consultation Management",
     description = "APIs for the clinical consultation workflow — start, save, complete, and retrieve patient encounters")
public class ConsultationController {

    private final ConsultationService consultationService;

    @Operation(
        summary = "Start a consultation",
        description = "Creates a DRAFT consultation for an IN_PROGRESS appointment. " +
                      "Only the doctor assigned to the appointment (or ADMIN) may call this. " +
                      "Returns 409 if a consultation already exists for the appointment."
    )
    @PostMapping("/start/{appointmentCode}")
    public ResponseEntity<ApiResponse<ConsultationResponseDTO>> startConsultation(
            @PathVariable String appointmentCode) {

        ConsultationResponseDTO response = consultationService.startConsultation(appointmentCode);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Consultation started successfully", response));
    }

    @Operation(
        summary = "Save consultation draft",
        description = "Persists clinical data (vitals, notes, prescriptions) to a DRAFT consultation. " +
                      "All fields are optional — supports partial saves. " +
                      "Rejected if the consultation is already COMPLETED."
    )
    @PutMapping("/{consultationCode}/draft")
    public ResponseEntity<ApiResponse<ConsultationResponseDTO>> saveDraft(
            @PathVariable String consultationCode,
            @Valid @RequestBody ConsultationSaveDTO request) {

        ConsultationResponseDTO response = consultationService.saveDraft(consultationCode, request);
        return ResponseEntity.ok(ApiResponse.success("Consultation draft saved successfully", response));
    }

    @Operation(
        summary = "Complete a consultation",
        description = "Finalises a DRAFT consultation. Validates required fields: chiefComplaint and diagnosis. " +
                      "If followUpRequired=true, followUpDate is also required. " +
                      "On completion: consultation status → COMPLETED, appointment status → COMPLETED. " +
                      "Completed consultations are locked — no further edits allowed."
    )
    @PutMapping("/{consultationCode}/complete")
    public ResponseEntity<ApiResponse<ConsultationResponseDTO>> completeConsultation(
            @PathVariable String consultationCode,
            @Valid @RequestBody ConsultationSaveDTO request) {

        ConsultationResponseDTO response =
                consultationService.completeConsultation(consultationCode, request);
        return ResponseEntity.ok(
                ApiResponse.success("Consultation completed successfully. Appointment marked as COMPLETED.", response));
    }

    @Operation(
        summary = "Get consultation by code",
        description = "Retrieves full consultation details including vitals, clinical notes, " +
                      "prescriptions, and appointment context. " +
                      "Access: ADMIN (all), DOCTOR (own), PATIENT (own)."
    )
    @GetMapping("/{consultationCode}")
    public ResponseEntity<ApiResponse<ConsultationResponseDTO>> getConsultationByCode(
            @PathVariable String consultationCode) {

        ConsultationResponseDTO response = consultationService.getConsultationByCode(consultationCode);
        return ResponseEntity.ok(ApiResponse.success("Consultation retrieved successfully", response));
    }

    @Operation(
        summary = "Get all consultations (Admin only)",
        description = "Returns a paginated list of all consultations in the system, newest first. " +
                      "Optionally filtered by status (DRAFT or COMPLETED). " +
                      "Access: ADMIN only — service layer enforces this."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConsultationResponseDTO>>> getAllConsultations(
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        if (size > 100) size = 100;
        Page<ConsultationResponseDTO> consultations =
                consultationService.getAllConsultations(status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Consultations retrieved successfully", consultations));
    }

    @Operation(
        summary = "Get consultations by patient",
        description = "Returns a paginated list of all consultations for a specific patient, newest first. " +
                      "Optionally filtered by status (DRAFT or COMPLETED). " +
                      "Access: ADMIN (all patients), DOCTOR (clinical context), PATIENT (own only)."
    )
    @GetMapping("/patient/{patientCode}")
    public ResponseEntity<ApiResponse<Page<ConsultationResponseDTO>>> getConsultationsByPatient(
            @PathVariable String patientCode,
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        if (size > 100) size = 100;
        Page<ConsultationResponseDTO> consultations =
                consultationService.getConsultationsByPatient(patientCode, status, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Patient consultations retrieved successfully", consultations));
    }

    @Operation(
        summary = "Get consultations by doctor",
        description = "Returns a paginated list of all consultations for a specific doctor, newest first. " +
                      "Optionally filtered by status (DRAFT or COMPLETED). " +
                      "Access: ADMIN (all doctors), DOCTOR (own only)."
    )
    @GetMapping("/doctor/{doctorCode}")
    public ResponseEntity<ApiResponse<Page<ConsultationResponseDTO>>> getConsultationsByDoctor(
            @PathVariable String doctorCode,
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        if (size > 100) size = 100;
        Page<ConsultationResponseDTO> consultations =
                consultationService.getConsultationsByDoctor(doctorCode, status, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Doctor consultations retrieved successfully", consultations));
    }
}
