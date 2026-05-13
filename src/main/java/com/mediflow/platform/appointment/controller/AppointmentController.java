package com.mediflow.platform.appointment.controller;

import com.mediflow.platform.appointment.dto.AppointmentRequestDTO;
import com.mediflow.platform.appointment.dto.AppointmentResponseDTO;
import com.mediflow.platform.appointment.dto.TimeSlotDTO;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.service.AppointmentService;
import com.mediflow.platform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management", description = "APIs for booking, retrieving, and managing patient appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(
        summary = "Get available time slots for a doctor on a date",
        description = "Returns 30-minute slots from 09:00 to 17:30 for the given doctor on the given date. " +
                      "Each slot is marked available=true/false based on existing non-cancelled appointments."
    )
    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<TimeSlotDTO>>> getAvailableSlots(
            @RequestParam String doctorCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TimeSlotDTO> slots = appointmentService.getAvailableSlots(doctorCode, date);
        return ResponseEntity.ok(ApiResponse.success("Available slots retrieved successfully", slots));
    }

    @Operation(
        summary = "Book a new appointment",
        description = "Validates patient and doctor availability, checks for scheduling conflicts, " +
                      "and creates a new appointment. Assigns a unique code (APT-YYYY-NNNN) automatically."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> bookAppointment(
            @Valid @RequestBody AppointmentRequestDTO request) {

        AppointmentResponseDTO response = appointmentService.bookAppointment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked successfully", response));
    }

    @Operation(
        summary = "Get all appointments",
        description = "Returns a paginated list of all appointments sorted by date (newest first). " +
                      "Optionally filter by status (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppointmentResponseDTO>>> getAllAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AppointmentStatus status) {

        if (size > 100) size = 100;
        Page<AppointmentResponseDTO> appointments = appointmentService.getAllAppointments(page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved successfully", appointments));
    }

    @Operation(
        summary = "Get appointment by code",
        description = "Retrieves full details of an appointment using the unique appointment code (e.g., APT-2026-0001)."
    )
    @GetMapping("/{appointmentCode}")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> getAppointmentByCode(
            @PathVariable String appointmentCode) {

        AppointmentResponseDTO response = appointmentService.getAppointmentByCode(appointmentCode);
        return ResponseEntity.ok(ApiResponse.success("Appointment retrieved successfully", response));
    }

    @Operation(
        summary = "Get appointments by patient",
        description = "Returns a paginated list of all appointments for a specific patient, " +
                      "sorted by appointment date (newest first)."
    )
    @GetMapping("/patient/{patientCode}")
    public ResponseEntity<ApiResponse<Page<AppointmentResponseDTO>>> getAppointmentsByPatient(
            @PathVariable String patientCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 100) size = 100;
        Page<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByPatient(patientCode, page, size);
        return ResponseEntity.ok(ApiResponse.success("Patient appointments retrieved successfully", appointments));
    }

    @Operation(
        summary = "Get appointments by doctor",
        description = "Returns a paginated list of all appointments for a specific doctor, " +
                      "sorted by appointment date (newest first)."
    )
    @GetMapping("/doctor/{doctorCode}")
    public ResponseEntity<ApiResponse<Page<AppointmentResponseDTO>>> getAppointmentsByDoctor(
            @PathVariable String doctorCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 100) size = 100;
        Page<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByDoctor(doctorCode, page, size);
        return ResponseEntity.ok(ApiResponse.success("Doctor appointments retrieved successfully", appointments));
    }

    @Operation(
        summary = "Cancel an appointment",
        description = "Cancels a SCHEDULED appointment. Appointments in any other status cannot be cancelled."
    )
    @PutMapping("/{appointmentCode}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> cancelAppointment(
            @PathVariable String appointmentCode) {

        AppointmentResponseDTO response = appointmentService.cancelAppointment(appointmentCode);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully", response));
    }

    @Operation(
        summary = "Complete an appointment",
        description = "Marks an IN_PROGRESS appointment as COMPLETED. " +
                      "Only appointments currently in progress can be completed."
    )
    @PutMapping("/{appointmentCode}/complete")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> completeAppointment(
            @PathVariable String appointmentCode) {

        AppointmentResponseDTO response = appointmentService.completeAppointment(appointmentCode);
        return ResponseEntity.ok(ApiResponse.success("Appointment completed successfully", response));
    }
}
