package com.mediflow.platform.appointment.service.impl;

import com.mediflow.platform.appointment.dto.AppointmentRequestDTO;
import com.mediflow.platform.appointment.dto.AppointmentResponseDTO;
import com.mediflow.platform.appointment.dto.TimeSlotDTO;
import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.enums.BookedBy;
import com.mediflow.platform.appointment.exception.AppointmentConflictException;
import com.mediflow.platform.appointment.exception.AppointmentNotFoundException;
import com.mediflow.platform.appointment.mapper.AppointmentMapper;
import com.mediflow.platform.appointment.repository.AppointmentRepository;
import com.mediflow.platform.appointment.service.AppointmentService;
import com.mediflow.platform.common.exception.BusinessRuleViolationException;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import com.mediflow.platform.doctor.exception.DoctorNotFoundException;
import com.mediflow.platform.doctor.repository.DoctorRepository;
import com.mediflow.platform.patient.entity.Patient;
import com.mediflow.platform.patient.enums.PatientStatus;
import com.mediflow.platform.patient.exception.PatientNotFoundException;
import com.mediflow.platform.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /**
     * Books a new appointment following the 9-step validation and persistence flow:
     * 1. Validate patient exists
     * 2. Validate patient is ACTIVE
     * 3. Validate doctor exists
     * 4. Validate doctor is ACTIVE
     * 5. Validate appointment date is today or future; start time is before end time
     * 6. Validate no overlapping doctor appointments
     * 7. Generate appointment code (APT-YYYY-NNNN)
     * 8. Populate snapshot fields from live entities
     * 9. Persist and return response DTO
     */
    @Override
    @Transactional
    public AppointmentResponseDTO bookAppointment(AppointmentRequestDTO request) {

        // Step 1 & 2: Patient validation
        Patient patient = patientRepository.findByPatientCode(request.getPatientCode())
                .orElseThrow(() -> new PatientNotFoundException(request.getPatientCode()));

        if (patient.getStatus() != PatientStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                "Patient " + request.getPatientCode() + " is not active and cannot book appointments."
            );
        }

        // Step 3 & 4: Doctor validation
        Doctor doctor = doctorRepository.findByDoctorCode(request.getDoctorCode())
                .orElseThrow(() -> new DoctorNotFoundException(request.getDoctorCode()));

        if (doctor.getStatus() != DoctorStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                "Doctor " + request.getDoctorCode() + " is not available for appointments. " +
                "Current status: " + doctor.getStatus()
            );
        }

        // Step 5: Date and time validation
        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleViolationException(
                "Appointment date must be today or a future date."
            );
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessRuleViolationException(
                "Start time must be before end time."
            );
        }

        // Step 6: Overlap detection
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
                doctor.getId(),
                request.getAppointmentDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!overlapping.isEmpty()) {
            throw new AppointmentConflictException(
                request.getDoctorCode(),
                request.getAppointmentDate(),
                request.getStartTime(),
                request.getEndTime()
            );
        }

        // Derive bookedBy from the JWT principal so the frontend never controls it
        if (request.getBookedBy() == null) {
            request.setBookedBy(deriveBookedBy());
        }

        // Steps 7, 8, 9: Generate code, build entity with snapshots, persist
        String appointmentCode = generateAppointmentCode();
        Appointment appointment = AppointmentMapper.toEntity(request, patient, doctor, appointmentCode);
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Appointment booked | code={}, patient={}, doctor={}, date={}",
                saved.getAppointmentCode(), request.getPatientCode(),
                request.getDoctorCode(), request.getAppointmentDate());

        return AppointmentMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentByCode(String appointmentCode) {
        Appointment appointment = appointmentRepository.findByAppointmentCode(appointmentCode)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentCode));
        return AppointmentMapper.toResponseDTO(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getAllAppointments(int page, int size, AppointmentStatus status) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "appointmentDate").and(Sort.by(Sort.Direction.DESC, "startTime")));
        if (status != null) {
            return appointmentRepository.findByAppointmentStatus(status, pageable)
                    .map(AppointmentMapper::toResponseDTO);
        }
        return appointmentRepository.findAll(pageable)
                .map(AppointmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getAppointmentsByPatient(String patientCode, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "appointmentDate").and(Sort.by(Sort.Direction.DESC, "startTime")));
        return appointmentRepository
                .findByPatient_PatientCode(patientCode, pageable)
                .map(AppointmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getAppointmentsByDoctor(String doctorCode, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "appointmentDate").and(Sort.by(Sort.Direction.DESC, "startTime")));
        return appointmentRepository
                .findByDoctor_DoctorCode(doctorCode, pageable)
                .map(AppointmentMapper::toResponseDTO);
    }

    /**
     * Cancels an appointment. Only appointments in SCHEDULED status can be cancelled.
     */
    @Override
    @Transactional
    public AppointmentResponseDTO cancelAppointment(String appointmentCode) {
        Appointment appointment = appointmentRepository.findByAppointmentCode(appointmentCode)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentCode));

        if (appointment.getAppointmentStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessRuleViolationException(
                "Only SCHEDULED appointments can be cancelled. Current status: " +
                appointment.getAppointmentStatus()
            );
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Appointment cancelled | code={}", appointmentCode);
        return AppointmentMapper.toResponseDTO(saved);
    }

    /**
     * Completes an appointment. Only appointments in IN_PROGRESS status can be completed.
     */
    @Override
    @Transactional
    public AppointmentResponseDTO completeAppointment(String appointmentCode) {
        Appointment appointment = appointmentRepository.findByAppointmentCode(appointmentCode)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentCode));

        if (appointment.getAppointmentStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException(
                "Only IN_PROGRESS appointments can be completed. Current status: " +
                appointment.getAppointmentStatus()
            );
        }

        appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Appointment completed | code={}", appointmentCode);
        return AppointmentMapper.toResponseDTO(saved);
    }

    // ── Available Slots ─────────────────────────────────────────────────────

    /**
     * Generates 30-minute slots from 09:00 to 17:30 for the given doctor on the given date,
     * marking each slot available or booked based on existing non-cancelled appointments.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getAvailableSlots(String doctorCode, LocalDate date) {
        Doctor doctor = doctorRepository.findByDoctorCode(doctorCode)
                .orElseThrow(() -> new DoctorNotFoundException(doctorCode));

        if (doctor.getStatus() != DoctorStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                "Doctor " + doctorCode + " is not available. Current status: " + doctor.getStatus()
            );
        }

        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleViolationException("Cannot fetch slots for a past date.");
        }

        List<Appointment> booked = appointmentRepository.findActiveByDoctorAndDate(doctor.getId(), date);

        List<TimeSlotDTO> slots = new ArrayList<>();
        LocalTime cursor  = LocalTime.of(9, 0);
        LocalTime dayEnd  = LocalTime.of(17, 30);
        Duration  step    = Duration.ofMinutes(30);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        while (!cursor.plus(step).isAfter(dayEnd)) {
            LocalTime slotEnd = cursor.plus(step);
            final LocalTime s = cursor;
            final LocalTime e = slotEnd;

            boolean isTaken = booked.stream()
                    .anyMatch(a -> a.getStartTime().isBefore(e) && a.getEndTime().isAfter(s));

            slots.add(TimeSlotDTO.builder()
                    .startTime(s.format(fmt))
                    .endTime(e.format(fmt))
                    .available(!isTaken)
                    .build());

            cursor = slotEnd;
        }

        return slots;
    }

    // ── BookedBy Derivation ─────────────────────────────────────────────────

    /**
     * Reads the current JWT principal's roles to determine the appropriate BookedBy value.
     * ADMIN role → ADMIN, PATIENT role → PATIENT, anything else (DOCTOR / unauthenticated) → SYSTEM.
     */
    private BookedBy deriveBookedBy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return BookedBy.SYSTEM;
        }
        boolean isAdmin   = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));

        if (isAdmin)   return BookedBy.ADMIN;
        if (isPatient) return BookedBy.PATIENT;
        return BookedBy.SYSTEM;
    }

    // ── Appointment Code Generation ─────────────────────────────────────────

    /**
     * Generates a year-scoped sequential appointment code.
     *
     * Format : APT-YYYY-NNNN
     * Example: APT-2026-0001, APT-2026-0042
     *
     * Mirrors the DoctorServiceImpl.generateDoctorCode() strategy exactly:
     * queries for the highest existing code with the current year prefix,
     * parses the sequence, increments, and zero-pads to 4 digits.
     */
    private String generateAppointmentCode() {
        String year   = String.valueOf(LocalDate.now().getYear());
        String prefix = "APT-" + year + "-";

        Optional<Appointment> latest = appointmentRepository
                .findTopByAppointmentCodeStartingWithOrderByAppointmentCodeDesc(prefix);

        int nextSequence = 1;
        if (latest.isPresent()) {
            String lastCode      = latest.get().getAppointmentCode();
            String sequencePart  = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(sequencePart) + 1;
        }

        if (nextSequence > 9999) {
            throw new IllegalStateException(
                "Appointment code sequence limit (9999) reached for year " + year +
                ". Contact the system administrator to expand the format."
            );
        }

        return prefix + String.format("%04d", nextSequence);
    }
}
