package com.mediflow.platform.consultation.service.impl;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.exception.AppointmentNotFoundException;
import com.mediflow.platform.appointment.repository.AppointmentRepository;
import com.mediflow.platform.auth.security.UserPrincipal;
import com.mediflow.platform.common.exception.BusinessRuleViolationException;
import com.mediflow.platform.consultation.dto.ConsultationResponseDTO;
import com.mediflow.platform.consultation.dto.ConsultationSaveDTO;
import com.mediflow.platform.consultation.dto.PrescriptionItemRequestDTO;
import com.mediflow.platform.consultation.entity.Consultation;
import com.mediflow.platform.consultation.entity.PrescriptionItem;
import com.mediflow.platform.consultation.enums.ConsultationStatus;
import com.mediflow.platform.consultation.exception.ConsultationAlreadyExistsException;
import com.mediflow.platform.consultation.exception.ConsultationNotFoundException;
import com.mediflow.platform.consultation.mapper.ConsultationMapper;
import com.mediflow.platform.consultation.repository.ConsultationRepository;
import com.mediflow.platform.consultation.service.ConsultationService;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.doctor.repository.DoctorRepository;
import com.mediflow.platform.patient.entity.Patient;
import com.mediflow.platform.patient.exception.PatientNotFoundException;
import com.mediflow.platform.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // ── Start Consultation ────────────────────────────────────────────────────────

    /**
     * Creates a DRAFT consultation from an IN_PROGRESS appointment.
     *
     * Rules enforced:
     *  1. Appointment must exist.
     *  2. Appointment must be IN_PROGRESS — rejects all other statuses.
     *  3. No consultation may already exist for this appointment (prevents duplicates).
     *  4. Logged-in DOCTOR must be the one assigned to this appointment.
     *     ADMIN can start consultations on behalf of any doctor.
     */
    @Override
    @Transactional
    public ConsultationResponseDTO startConsultation(String appointmentCode) {
        Appointment appointment = appointmentRepository.findByAppointmentCode(appointmentCode)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentCode));

        if (appointment.getAppointmentStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException(
                "Consultation can only be started for IN_PROGRESS appointments. " +
                "Current status: " + appointment.getAppointmentStatus() +
                ". Ensure the appointment has been started (CONFIRMED → IN_PROGRESS) before beginning the clinical encounter."
            );
        }

        // Prevent duplicate consultations for the same appointment
        if (consultationRepository.existsByAppointment_AppointmentCode(appointmentCode)) {
            Consultation existing = consultationRepository
                    .findByAppointment_AppointmentCode(appointmentCode).get();
            throw new ConsultationAlreadyExistsException(appointmentCode, existing.getConsultationCode());
        }

        // Ownership guard: DOCTOR role must be the assigned doctor for this appointment
        validateDoctorOwnership(appointment);

        String consultationCode = generateConsultationCode();

        Consultation consultation = Consultation.builder()
                .consultationCode(consultationCode)
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .patientNameSnapshot(appointment.getPatientNameSnapshot())
                .doctorNameSnapshot(appointment.getDoctorNameSnapshot())
                .consultationStatus(ConsultationStatus.DRAFT)
                .followUpRequired(false)
                .prescriptionItems(new ArrayList<>())
                .build();

        Consultation saved = consultationRepository.save(consultation);

        log.info("Consultation started | code={}, appointment={}, doctor={}, patient={}",
                consultationCode, appointmentCode,
                appointment.getDoctor().getDoctorCode(),
                appointment.getPatient().getPatientCode());

        return ConsultationMapper.toResponseDTO(saved);
    }

    // ── Save Draft ────────────────────────────────────────────────────────────────

    /**
     * Persists clinical data to a DRAFT consultation — no required fields.
     * Replaces prescription items atomically (clear + re-add within same transaction).
     * Rejects if consultation is already COMPLETED.
     */
    @Override
    @Transactional
    public ConsultationResponseDTO saveDraft(String consultationCode, ConsultationSaveDTO request) {
        Consultation consultation = loadAndValidateForEdit(consultationCode);

        applyFields(consultation, request);

        // Auto-calculate BMI if not supplied but height + weight are present
        if (request.getBmi() == null &&
                consultation.getHeight() != null && consultation.getHeight().compareTo(BigDecimal.ZERO) > 0 &&
                consultation.getWeight() != null && consultation.getWeight().compareTo(BigDecimal.ZERO) > 0) {
            consultation.setBmi(calculateBmi(consultation.getHeight(), consultation.getWeight()));
        }

        replacePrescriptions(consultation, request.getPrescriptionItems());

        Consultation saved = consultationRepository.save(consultation);

        log.info("Consultation draft saved | code={}", consultationCode);
        return ConsultationMapper.toResponseDTO(saved);
    }

    // ── Complete Consultation ─────────────────────────────────────────────────────

    /**
     * Finalises a DRAFT consultation.
     *
     * Additional validations (beyond draft):
     *  - chiefComplaint must be non-blank
     *  - diagnosis must be non-blank
     *  - if followUpRequired = true, followUpDate must be supplied
     *  - prescription items must each have medicineName, dosage, frequency, duration
     *
     * On completion:
     *  - Consultation status → COMPLETED
     *  - Linked appointment status → COMPLETED (both saved atomically)
     */
    @Override
    @Transactional
    public ConsultationResponseDTO completeConsultation(String consultationCode, ConsultationSaveDTO request) {
        Consultation consultation = loadAndValidateForEdit(consultationCode);

        applyFields(consultation, request);

        if (consultation.getBmi() == null &&
                consultation.getHeight() != null && consultation.getHeight().compareTo(BigDecimal.ZERO) > 0 &&
                consultation.getWeight() != null && consultation.getWeight().compareTo(BigDecimal.ZERO) > 0) {
            consultation.setBmi(calculateBmi(consultation.getHeight(), consultation.getWeight()));
        }

        replacePrescriptions(consultation, request.getPrescriptionItems());

        // ── Strict completion validations ──
        if (!StringUtils.hasText(consultation.getChiefComplaint())) {
            throw new BusinessRuleViolationException(
                "Chief complaint is required to complete a consultation.");
        }
        if (!StringUtils.hasText(consultation.getDiagnosis())) {
            throw new BusinessRuleViolationException(
                "Diagnosis is required to complete a consultation.");
        }
        if (Boolean.TRUE.equals(consultation.getFollowUpRequired())
                && consultation.getFollowUpDate() == null) {
            throw new BusinessRuleViolationException(
                "Follow-up date is required when follow-up is marked as required.");
        }
        validatePrescriptionItemsForCompletion(consultation.getPrescriptionItems());

        // ── Verify appointment is still completable (not cancelled during consultation) ──
        Appointment appointment = consultation.getAppointment();
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                "Cannot complete consultation — the linked appointment has been cancelled.");
        }

        consultation.setConsultationStatus(ConsultationStatus.COMPLETED);

        // Synchronise appointment status to COMPLETED
        appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        Consultation saved = consultationRepository.save(consultation);

        log.info("Consultation completed | code={}, appointment={}", consultationCode,
                appointment.getAppointmentCode());

        return ConsultationMapper.toResponseDTO(saved);
    }

    // ── Read Operations ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ConsultationResponseDTO getConsultationByCode(String consultationCode) {
        Consultation consultation = consultationRepository.findByConsultationCode(consultationCode)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationCode));

        validateReadAccess(consultation);
        return ConsultationMapper.toResponseDTO(consultation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationResponseDTO> getAllConsultations(
            ConsultationStatus status, int page, int size) {

        validateAdminAccess();

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return consultationRepository.findByConsultationStatus(status, pageable)
                    .map(ConsultationMapper::toResponseDTO);
        }
        return consultationRepository.findAll(pageable)
                .map(ConsultationMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationResponseDTO> getConsultationsByPatient(
            String patientCode, ConsultationStatus status, int page, int size) {

        patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));

        validatePatientReadAccess(patientCode);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return consultationRepository.findByPatient_PatientCodeAndConsultationStatus(
                    patientCode, status, pageable)
                    .map(ConsultationMapper::toResponseDTO);
        }
        return consultationRepository.findByPatient_PatientCode(patientCode, pageable)
                .map(ConsultationMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationResponseDTO> getConsultationsByDoctor(
            String doctorCode, ConsultationStatus status, int page, int size) {

        doctorRepository.findByDoctorCode(doctorCode)
                .orElseThrow(() -> new com.mediflow.platform.doctor.exception.DoctorNotFoundException(doctorCode));

        validateDoctorReadAccess(doctorCode);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return consultationRepository.findByDoctor_DoctorCodeAndConsultationStatus(
                    doctorCode, status, pageable)
                    .map(ConsultationMapper::toResponseDTO);
        }
        return consultationRepository.findByDoctor_DoctorCode(doctorCode, pageable)
                .map(ConsultationMapper::toResponseDTO);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────────

    /**
     * Loads consultation, verifies it is editable (DRAFT), and validates doctor ownership.
     */
    private Consultation loadAndValidateForEdit(String consultationCode) {
        Consultation consultation = consultationRepository.findByConsultationCode(consultationCode)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationCode));

        if (consultation.getConsultationStatus() == ConsultationStatus.COMPLETED) {
            throw new BusinessRuleViolationException(
                "Consultation " + consultationCode +
                " is already COMPLETED and cannot be modified. " +
                "Completed consultations are locked to preserve clinical record integrity.");
        }

        validateDoctorOwnership(consultation.getAppointment());
        return consultation;
    }

    /**
     * Copies all non-null DTO fields onto the consultation entity.
     * Allows partial updates — null fields are not overwritten (draft-safe).
     */
    private void applyFields(Consultation c, ConsultationSaveDTO dto) {
        if (dto.getBloodPressure()    != null) c.setBloodPressure(dto.getBloodPressure());
        if (dto.getPulseRate()        != null) c.setPulseRate(dto.getPulseRate());
        if (dto.getTemperature()      != null) c.setTemperature(dto.getTemperature());
        if (dto.getOxygenSaturation() != null) c.setOxygenSaturation(dto.getOxygenSaturation());
        if (dto.getRespiratoryRate()  != null) c.setRespiratoryRate(dto.getRespiratoryRate());
        if (dto.getHeight()           != null) c.setHeight(dto.getHeight());
        if (dto.getWeight()           != null) c.setWeight(dto.getWeight());
        if (dto.getBmi()              != null) c.setBmi(dto.getBmi());

        if (dto.getChiefComplaint() != null) c.setChiefComplaint(dto.getChiefComplaint());
        if (dto.getSymptoms()       != null) c.setSymptoms(dto.getSymptoms());
        if (dto.getDiagnosis()      != null) c.setDiagnosis(dto.getDiagnosis());
        if (dto.getDoctorNotes()    != null) c.setDoctorNotes(dto.getDoctorNotes());
        if (dto.getTreatmentPlan()  != null) c.setTreatmentPlan(dto.getTreatmentPlan());

        if (dto.getFollowUpRequired() != null) c.setFollowUpRequired(dto.getFollowUpRequired());
        if (dto.getFollowUpDate()     != null) c.setFollowUpDate(dto.getFollowUpDate());
        if (dto.getFollowUpNotes()    != null) c.setFollowUpNotes(dto.getFollowUpNotes());

        // Clear follow-up date when follow-up is explicitly set to false
        if (Boolean.FALSE.equals(dto.getFollowUpRequired())) {
            c.setFollowUpDate(null);
        }
    }

    /**
     * Replaces all prescription items on a consultation atomically.
     * If the request contains no items (null or empty), existing items are preserved for drafts.
     * An explicit empty list clears all items.
     */
    private void replacePrescriptions(Consultation consultation,
                                      List<PrescriptionItemRequestDTO> items) {
        if (items == null) {
            return; // null = no change (draft partial save)
        }

        consultation.getPrescriptionItems().clear();

        for (PrescriptionItemRequestDTO itemDTO : items) {
            PrescriptionItem item = PrescriptionItem.builder()
                    .consultation(consultation)
                    .medicineName(itemDTO.getMedicineName().trim())
                    .dosage(itemDTO.getDosage() != null ? itemDTO.getDosage().trim() : null)
                    .frequency(itemDTO.getFrequency() != null ? itemDTO.getFrequency().trim() : null)
                    .duration(itemDTO.getDuration() != null ? itemDTO.getDuration().trim() : null)
                    .instructions(itemDTO.getInstructions() != null
                            ? itemDTO.getInstructions().trim() : null)
                    .build();
            consultation.getPrescriptionItems().add(item);
        }
    }

    /**
     * Validates prescription items for consultation completion:
     * each item must have medicineName, dosage, frequency, and duration.
     */
    private void validatePrescriptionItemsForCompletion(List<PrescriptionItem> items) {
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            PrescriptionItem item = items.get(i);
            if (!StringUtils.hasText(item.getMedicineName())) {
                throw new BusinessRuleViolationException(
                    "Prescription item " + (i + 1) + ": medicine name is required.");
            }
            if (!StringUtils.hasText(item.getDosage())) {
                throw new BusinessRuleViolationException(
                    "Prescription item " + (i + 1) + " (" + item.getMedicineName() + "): dosage is required.");
            }
            if (!StringUtils.hasText(item.getFrequency())) {
                throw new BusinessRuleViolationException(
                    "Prescription item " + (i + 1) + " (" + item.getMedicineName() + "): frequency is required.");
            }
            if (!StringUtils.hasText(item.getDuration())) {
                throw new BusinessRuleViolationException(
                    "Prescription item " + (i + 1) + " (" + item.getMedicineName() + "): duration is required.");
            }
        }
    }

    /**
     * BMI = weight (kg) / (height (m))²
     * Height in DTO is stored in centimetres; convert to metres before calculation.
     */
    private BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }

    // ── Security Helpers ──────────────────────────────────────────────────────────

    /** Throws AccessDeniedException unless the caller has ROLE_ADMIN. */
    private void validateAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required.");
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required.");
        }
    }

    /**
     * Ownership guard for write operations (start, save draft, complete).
     *
     * ADMIN — unrestricted access.
     * DOCTOR — must be the doctor assigned to the appointment.
     * All others — access denied.
     */
    private void validateDoctorOwnership(Appointment appointment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required.");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (!isDoctor) {
            throw new AccessDeniedException(
                "Only doctors and administrators can create or modify consultations.");
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        // The appointment's doctor must have a linked user account whose email matches the caller
        Doctor assignedDoctor = appointment.getDoctor();
        if (assignedDoctor.getUser() == null ||
                !loggedInEmail.equals(assignedDoctor.getUser().getEmail())) {
            log.warn("[Security] Consultation ownership violation | user={} attempted access to appointment={}",
                    loggedInEmail, appointment.getAppointmentCode());
            throw new AccessDeniedException(
                "Access denied. You can only manage consultations for your own appointments.");
        }
    }

    /**
     * Read-access guard for a specific consultation.
     *
     * ADMIN  — full access.
     * DOCTOR — must be the consultation's assigned doctor.
     * PATIENT — must be the patient on the consultation.
     */
    private void validateReadAccess(Consultation consultation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required.");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            Doctor assignedDoctor = consultation.getDoctor();
            if (assignedDoctor.getUser() == null ||
                    !loggedInEmail.equals(assignedDoctor.getUser().getEmail())) {
                throw new AccessDeniedException(
                    "Access denied. You can only view your own consultation records.");
            }
            return;
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            Patient patient = consultation.getPatient();
            if (patient.getUser() == null || !loggedInEmail.equals(patient.getUser().getEmail())) {
                log.warn("[Security] Patient consultation access violation | user={}, consultation={}",
                        loggedInEmail, consultation.getConsultationCode());
                throw new AccessDeniedException(
                    "Access denied. You can only view your own consultation records.");
            }
            return;
        }

        throw new AccessDeniedException("Access denied.");
    }

    /**
     * Read-access guard for patient-scoped consultation list.
     *
     * ADMIN  — full access.
     * PATIENT — can only query their own patient code.
     * DOCTOR  — can see consultations of their own patients (allowed for clinical context).
     */
    private void validatePatientReadAccess(String patientCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) return; // Doctors can view patient consultation history for clinical context

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String loggedInEmail = principal.getEmail();
            Patient patient = patientRepository.findByPatientCode(patientCode)
                    .orElseThrow(() -> new PatientNotFoundException(patientCode));
            if (patient.getUser() == null || !loggedInEmail.equals(patient.getUser().getEmail())) {
                throw new AccessDeniedException(
                    "Access denied. You can only view your own consultation history.");
            }
        }
    }

    /**
     * Read-access guard for doctor-scoped consultation list.
     *
     * ADMIN  — full access.
     * DOCTOR — can only query their own doctorCode.
     */
    private void validateDoctorReadAccess(String doctorCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String loggedInEmail = principal.getEmail();
            Optional<Doctor> callerDoctor = doctorRepository.findByUser_Email(loggedInEmail);
            if (callerDoctor.isEmpty() || !doctorCode.equals(callerDoctor.get().getDoctorCode())) {
                throw new AccessDeniedException(
                    "Access denied. You can only view your own consultation records.");
            }
        }
    }

    // ── Consultation Code Generation ──────────────────────────────────────────────

    /**
     * Generates a year-scoped sequential consultation code.
     *
     * Format : CONS-YYYY-NNNN
     * Example: CONS-2026-0001, CONS-2026-0042
     *
     * Mirrors the AppointmentServiceImpl.generateAppointmentCode() strategy exactly.
     */
    private String generateConsultationCode() {
        String year   = String.valueOf(LocalDate.now().getYear());
        String prefix = "CONS-" + year + "-";

        Optional<Consultation> latest = consultationRepository
                .findTopByConsultationCodeStartingWithOrderByConsultationCodeDesc(prefix);

        int nextSequence = 1;
        if (latest.isPresent()) {
            String lastCode     = latest.get().getConsultationCode();
            String sequencePart = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(sequencePart) + 1;
        }

        if (nextSequence > 9999) {
            throw new IllegalStateException(
                "Consultation code sequence limit (9999) reached for year " + year +
                ". Contact the system administrator to expand the format."
            );
        }

        return prefix + String.format("%04d", nextSequence);
    }
}
