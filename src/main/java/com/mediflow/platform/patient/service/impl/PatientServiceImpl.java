package com.mediflow.platform.patient.service.impl;

import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.repository.AppointmentRepository;
import com.mediflow.platform.auth.entity.Role;
import com.mediflow.platform.auth.entity.User;
import com.mediflow.platform.auth.enums.RoleName;
import com.mediflow.platform.auth.enums.UserStatus;
import com.mediflow.platform.auth.repository.RoleRepository;
import com.mediflow.platform.auth.repository.UserRepository;
import com.mediflow.platform.auth.service.impl.AuthServiceImpl;
import com.mediflow.platform.common.exception.BusinessRuleViolationException;
import com.mediflow.platform.patient.dto.PatientCreationResponseDTO;
import com.mediflow.platform.patient.dto.PatientRequestDTO;
import com.mediflow.platform.patient.dto.PatientResponseDTO;
import com.mediflow.platform.patient.entity.Patient;
import com.mediflow.platform.patient.enums.PatientStatus;
import com.mediflow.platform.patient.exception.PatientAlreadyExistsException;
import com.mediflow.platform.patient.exception.PatientNotFoundException;
import com.mediflow.platform.patient.mapper.PatientMapper;
import com.mediflow.platform.patient.repository.PatientRepository;
import com.mediflow.platform.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceImpl authServiceImpl;

    /**
     * Registers a new patient and creates the linked User identity in a single transaction.
     *
     * Steps:
     *  1. Validate email uniqueness (patients table and users table).
     *  2. Generate patient code (PAT-XXXXXXXXXX) and patient entity.
     *  3. Create User with PATIENT role and BCrypt-encoded temp password.
     *  4. Link patient.user = savedUser.
     *  5. Return PatientCreationResponseDTO with patient info + one-time temp password.
     */
    @Override
    @Transactional
    public PatientCreationResponseDTO createPatient(PatientRequestDTO request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        String normalizedPhone = request.getPhoneNumber().trim();

        log.debug("[PatientService] Creating patient for email='{}'", normalizedEmail);

        // Step 1: Email uniqueness check in patients table
        if (patientRepository.existsByEmail(normalizedEmail)) {
            throw new PatientAlreadyExistsException(normalizedEmail);
        }

        // The same email becomes the User account — must also be unique in users table
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new PatientAlreadyExistsException(normalizedEmail);
        }

        // Step 2: Generate code and build patient entity
        String patientCode = generateUniquePatientCode();
        Patient patient = PatientMapper.toEntity(request, patientCode);

        // Step 3: Create User account with PATIENT role and temporary password
        Role patientRole = roleRepository.findByName(RoleName.PATIENT)
                .orElseThrow(() -> new IllegalStateException("PATIENT role not found. Ensure data.sql seeded the roles."));

        String tempPassword    = authServiceImpl.generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        String username        = authServiceImpl.deriveUniqueUsername(normalizedEmail);

        User user = User.builder()
                .username(username)
                .email(normalizedEmail)
                .phoneNumber(normalizedPhone)
                .password(encodedPassword)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(patientRole))
                .build();
        User savedUser = userRepository.save(user);
        log.debug("[PatientService] User account created for patient | username='{}'", username);

        // Step 4: Link patient to its User identity and persist
        patient.setUser(savedUser);
        Patient savedPatient = patientRepository.save(patient);

        log.info("[PatientService] Patient registered | code={} username='{}'",
                savedPatient.getPatientCode(), username);

        return PatientCreationResponseDTO.builder()
                .patient(PatientMapper.toResponseDTO(savedPatient))
                .temporaryPassword(tempPassword)
                .username(username)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientByCode(String patientCode) {
        Patient patient = patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));
        if (patient.getStatus() == PatientStatus.INACTIVE) {
            throw new PatientNotFoundException(patientCode);
        }
        return PatientMapper.toResponseDTO(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponseDTO> getAllPatients(int page, int size, PatientStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        PatientStatus effectiveStatus = (status != null) ? status : PatientStatus.ACTIVE;
        Page<Patient> patients = patientRepository.findAllByStatus(effectiveStatus, pageable);

        return patients.map(PatientMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public PatientResponseDTO updatePatient(String patientCode, PatientRequestDTO request) {
        Patient patient = patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (patientRepository.existsByEmailAndPatientCodeNot(normalizedEmail, patientCode)) {
            throw new PatientAlreadyExistsException(normalizedEmail);
        }

        PatientMapper.updateEntityFromDTO(patient, request);
        Patient updatedPatient = patientRepository.save(patient);

        log.info("Patient updated successfully with code: {}", updatedPatient.getPatientCode());
        return PatientMapper.toResponseDTO(updatedPatient);
    }

    @Override
    @Transactional
    public PatientResponseDTO deactivatePatient(String patientCode) {
        Patient patient = patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));

        boolean hasFutureAppointments = appointmentRepository
                .existsByPatient_PatientCodeAndAppointmentDateAfterAndAppointmentStatusIn(
                        patientCode,
                        LocalDate.now(),
                        List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.IN_PROGRESS)
                );

        if (hasFutureAppointments) {
            throw new BusinessRuleViolationException(
                "Patient " + patientCode + " has upcoming appointments. " +
                "Cancel all future appointments before deactivating the patient."
            );
        }

        patient.setStatus(PatientStatus.INACTIVE);
        Patient saved = patientRepository.save(patient);

        log.info("Patient soft-deleted (deactivated) with code: {}", patientCode);
        return PatientMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponseDTO> searchPatients(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firstName"));
        return patientRepository.searchActivePatients(query, pageable)
                .map(PatientMapper::toResponseDTO);
    }

    private String generateUniquePatientCode() {
        String code;
        do {
            code = "PAT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (patientRepository.existsByPatientCode(code));
        return code;
    }
}
