package com.mediflow.platform.doctor.service.impl;

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
import com.mediflow.platform.doctor.dto.DoctorCreationResponseDTO;
import com.mediflow.platform.doctor.dto.DoctorRequestDTO;
import com.mediflow.platform.doctor.dto.DoctorResponseDTO;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import com.mediflow.platform.doctor.exception.DoctorAlreadyExistsException;
import com.mediflow.platform.doctor.exception.DoctorNotFoundException;
import com.mediflow.platform.doctor.mapper.DoctorMapper;
import com.mediflow.platform.doctor.repository.DoctorRepository;
import com.mediflow.platform.doctor.service.DoctorService;
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
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceImpl authServiceImpl;

    /**
     * Registers a new doctor and creates the linked User identity in a single transaction.
     *
     * Steps:
     *  1. Validate email and license uniqueness (doctor table).
     *  2. Validate email uniqueness (users table) — the same email becomes the login email.
     *  3. Generate doctor code (DOC-YYYY-NNNN) and doctor entity.
     *  4. Create User with DOCTOR role and BCrypt-encoded temp password.
     *  5. Link doctor.user = savedUser.
     *  6. Return DoctorCreationResponseDTO containing doctor info + one-time temp password.
     *
     * The entire operation is atomic — if User creation fails, Doctor is not persisted.
     */
    @Override
    @Transactional
    public DoctorCreationResponseDTO createDoctor(DoctorRequestDTO request) {
        String normalizedEmail   = request.getEmail().toLowerCase().trim();
        String normalizedLicense = request.getLicenseNumber().trim().toUpperCase();
        String normalizedPhone   = request.getPhoneNumber().trim();

        log.debug("[DoctorService] Creating doctor for email='{}'", normalizedEmail);

        // Step 1: Email must be unique in the doctors table
        if (doctorRepository.existsByEmail(normalizedEmail)) {
            throw new DoctorAlreadyExistsException("email", normalizedEmail);
        }

        // License number is a government-issued unique identifier per practitioner
        if (doctorRepository.existsByLicenseNumber(normalizedLicense)) {
            throw new DoctorAlreadyExistsException("license number", normalizedLicense);
        }

        // Step 2: The same email becomes the User account email — must be globally unique
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DoctorAlreadyExistsException("email", normalizedEmail);
        }

        // Step 3: Generate code and build doctor entity
        String doctorCode = generateDoctorCode();
        Doctor doctor = DoctorMapper.toEntity(request, doctorCode);

        // Step 4: Create User account with DOCTOR role and temporary password
        Role doctorRole = roleRepository.findByName(RoleName.DOCTOR)
                .orElseThrow(() -> new IllegalStateException("DOCTOR role not found. Ensure data.sql seeded the roles."));

        String tempPassword    = authServiceImpl.generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        String username        = authServiceImpl.deriveUniqueUsername(normalizedEmail);

        User user = User.builder()
                .username(username)
                .email(normalizedEmail)
                .phoneNumber(normalizedPhone)
                .password(encodedPassword)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(doctorRole))
                .build();
        User savedUser = userRepository.save(user);
        log.debug("[DoctorService] User account created for doctor | username='{}'", username);

        // Step 5: Link doctor entity to its User identity and persist
        doctor.setUser(savedUser);
        Doctor saved = doctorRepository.save(doctor);

        log.info("[DoctorService] Doctor registered | code={} username='{}' department={}",
                saved.getDoctorCode(), username, saved.getDepartment());

        return DoctorCreationResponseDTO.builder()
                .doctor(DoctorMapper.toResponseDTO(saved))
                .temporaryPassword(tempPassword)
                .username(username)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponseDTO getDoctorByCode(String doctorCode) {
        Doctor doctor = doctorRepository.findByDoctorCode(doctorCode)
                .orElseThrow(() -> new DoctorNotFoundException(doctorCode));
        if (doctor.getStatus() == DoctorStatus.INACTIVE) {
            throw new DoctorNotFoundException(doctorCode);
        }
        return DoctorMapper.toResponseDTO(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponseDTO> getAllDoctors(int page, int size, DoctorStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Doctor> doctors = (status != null)
                ? doctorRepository.findAllByStatus(status, pageable)
                : doctorRepository.findAll(pageable);

        return doctors.map(DoctorMapper::toResponseDTO);
    }

    /**
     * Fully replaces a doctor's mutable fields. The doctorCode and createdAt are immutable.
     * Uniqueness of email and license is re-checked, excluding the current record.
     */
    @Override
    @Transactional
    public DoctorResponseDTO updateDoctor(String doctorCode, DoctorRequestDTO request) {
        Doctor doctor = doctorRepository.findByDoctorCode(doctorCode)
                .orElseThrow(() -> new DoctorNotFoundException(doctorCode));

        String normalizedEmail   = request.getEmail().toLowerCase().trim();
        String normalizedLicense = request.getLicenseNumber().trim().toUpperCase();

        // Ensure the updated email is not already used by a different doctor
        if (doctorRepository.existsByEmailAndDoctorCodeNot(normalizedEmail, doctorCode)) {
            throw new DoctorAlreadyExistsException("email", normalizedEmail);
        }

        // Ensure the updated license is not already used by a different doctor
        if (doctorRepository.existsByLicenseNumberAndDoctorCodeNot(normalizedLicense, doctorCode)) {
            throw new DoctorAlreadyExistsException("license number", normalizedLicense);
        }

        DoctorMapper.updateEntityFromDTO(doctor, request);
        Doctor updated = doctorRepository.save(doctor);

        log.info("Doctor updated successfully | code={}", updated.getDoctorCode());
        return DoctorMapper.toResponseDTO(updated);
    }

    /**
     * Soft-deletes a doctor by setting status to INACTIVE.
     * The record is retained for audit and referential integrity with existing appointments.
     */
    @Override
    @Transactional
    public DoctorResponseDTO deactivateDoctor(String doctorCode) {
        Doctor doctor = doctorRepository.findByDoctorCode(doctorCode)
                .orElseThrow(() -> new DoctorNotFoundException(doctorCode));

        boolean hasFutureAppointments = appointmentRepository
                .existsByDoctor_DoctorCodeAndAppointmentDateAfterAndAppointmentStatusIn(
                        doctorCode,
                        LocalDate.now(),
                        List.of(AppointmentStatus.PAYMENT_PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)
                );

        if (hasFutureAppointments) {
            throw new BusinessRuleViolationException(
                "Doctor " + doctorCode + " has upcoming appointments. " +
                "Cancel all future appointments before deactivating the doctor."
            );
        }

        doctor.setStatus(DoctorStatus.INACTIVE);
        Doctor saved = doctorRepository.save(doctor);

        log.info("Doctor soft-deleted (INACTIVE) | code={}", doctorCode);
        return DoctorMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponseDTO> searchDoctors(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firstName"));
        return doctorRepository.searchActiveDoctors(query, pageable)
                .map(DoctorMapper::toResponseDTO);
    }

    // ── Doctor Code Generation ──────────────────────────────────────────────

    /**
     * Generates a human-readable, year-scoped sequential doctor code.
     *
     * Format : DOC-YYYY-NNNN
     * Example: DOC-2026-0001, DOC-2026-0042
     *
     * The sequence resets at the start of each calendar year, making it easy
     * to identify when a doctor was onboarded.
     *
     * How it works:
     *   1. Queries the DB for the highest existing code with the current year prefix.
     *   2. Parses the 4-digit sequence number from the end of that code.
     *   3. Increments by 1 and zero-pads back to 4 digits.
     *
     * Concurrency note:
     *   This implementation is safe for single-server deployments — the @Transactional
     *   boundary combined with the DB unique constraint on doctor_code prevents duplicates.
     *   For multi-instance (horizontally scaled) deployments, replace this with a
     *   database sequence or a distributed counter (e.g., Redis INCR).
     */
    private String generateDoctorCode() {
        String year   = String.valueOf(LocalDate.now().getYear()); // e.g., "2026"
        String prefix = "DOC-" + year + "-";                      // e.g., "DOC-2026-"

        Optional<Doctor> lastDoctor = doctorRepository
                .findTopByDoctorCodeStartingWithOrderByDoctorCodeDesc(prefix);

        int nextSequence = 1;
        if (lastDoctor.isPresent()) {
            // Extract trailing sequence: "DOC-2026-0042" → "0042" → 42 → next: 43
            String lastCode    = lastDoctor.get().getDoctorCode();
            String sequencePart = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(sequencePart) + 1;
        }

        // Guard: 4-digit format supports up to 9999 doctors per year
        if (nextSequence > 9999) {
            throw new IllegalStateException(
                "Doctor code sequence limit (9999) reached for year " + year +
                ". Contact the system administrator to expand the format."
            );
        }

        return prefix + String.format("%04d", nextSequence); // e.g., "DOC-2026-0043"
    }
}
