package com.mediflow.platform.lab.service.impl;

import com.mediflow.platform.auth.security.UserPrincipal;
import com.mediflow.platform.common.exception.BusinessRuleViolationException;
import com.mediflow.platform.consultation.entity.Consultation;
import com.mediflow.platform.consultation.enums.ConsultationStatus;
import com.mediflow.platform.consultation.exception.ConsultationNotFoundException;
import com.mediflow.platform.consultation.repository.ConsultationRepository;
import com.mediflow.platform.doctor.exception.DoctorNotFoundException;
import com.mediflow.platform.doctor.repository.DoctorRepository;
import com.mediflow.platform.lab.dto.LabOrderItemRequestDTO;
import com.mediflow.platform.lab.dto.LabOrderRequestDTO;
import com.mediflow.platform.lab.dto.LabOrderResponseDTO;
import com.mediflow.platform.lab.dto.LabOrderStatusUpdateDTO;
import com.mediflow.platform.lab.entity.LabOrder;
import com.mediflow.platform.lab.entity.LabOrderItem;
import com.mediflow.platform.lab.exception.LabOrderNotFoundException;
import com.mediflow.platform.lab.mapper.LabOrderMapper;
import com.mediflow.platform.lab.repository.LabOrderRepository;
import com.mediflow.platform.lab.repository.LabReportRepository;
import com.mediflow.platform.lab.service.LabOrderService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabOrderServiceImpl implements LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final LabReportRepository labReportRepository;
    private final ConsultationRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // ── Create Lab Order ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabOrderResponseDTO createLabOrder(LabOrderRequestDTO request) {
        // Load consultation — derives patient and doctor automatically
        Consultation consultation = consultationRepository
                .findByConsultationCode(request.getConsultationCode())
                .orElseThrow(() -> new ConsultationNotFoundException(request.getConsultationCode()));

        // Lab orders can be created for both DRAFT and COMPLETED consultations
        // (doctor orders tests during consultation or after)
        if (consultation.getConsultationStatus() == ConsultationStatus.COMPLETED) {
            // Allowed — tests may be ordered post-consultation for follow-up
        }
        // Validate doctor ownership: only the assigned doctor or ADMIN may order tests
        validateDoctorOwnership(consultation);

        // Prevent duplicate test names within a single order
        validateNoDuplicateTests(request.getItems());

        String labOrderCode = generateLabOrderCode();

        LabOrder order = LabOrder.builder()
                .labOrderCode(labOrderCode)
                .consultation(consultation)
                .appointment(consultation.getAppointment())
                .patient(consultation.getPatient())
                .doctor(consultation.getDoctor())
                .orderDate(LocalDate.now())
                .priority(request.getPriority())
                .clinicalNotes(request.getClinicalNotes())
                .instructions(request.getInstructions())
                .items(new ArrayList<>())
                .build();

        // Attach order items
        for (LabOrderItemRequestDTO itemDTO : request.getItems()) {
            LabOrderItem item = LabOrderItem.builder()
                    .labOrder(order)
                    .testCode(itemDTO.getTestCode())
                    .testName(itemDTO.getTestName().trim())
                    .category(itemDTO.getCategory())
                    .remarks(itemDTO.getRemarks())
                    .build();
            order.getItems().add(item);
        }

        LabOrder saved = labOrderRepository.save(order);

        log.info("Lab order created | code={}, consultation={}, patient={}, doctor={}, tests={}",
                labOrderCode,
                request.getConsultationCode(),
                consultation.getPatient().getPatientCode(),
                consultation.getDoctor().getDoctorCode(),
                request.getItems().size());

        return LabOrderMapper.toResponseDTO(saved, labReportRepository);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public LabOrderResponseDTO getLabOrderByCode(String labOrderCode) {
        LabOrder order = labOrderRepository.findByLabOrderCode(labOrderCode)
                .orElseThrow(() -> new LabOrderNotFoundException(labOrderCode));
        validateReadAccess(order);
        return LabOrderMapper.toResponseDTO(order, labReportRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderResponseDTO> getAllLabOrders(String search, String status, int page, int size) {
        Authentication auth = getAuthentication();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String term = (search != null && !search.isBlank()) ? search.trim() : "";

        com.mediflow.platform.lab.enums.LabOrderStatus statusFilter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                statusFilter = com.mediflow.platform.lab.enums.LabOrderStatus.valueOf(
                    status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) { }
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            if (statusFilter != null) {
                return labOrderRepository.findByStatusAndSearch(statusFilter, term, pageable)
                        .map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
            }
            return term.isEmpty()
                ? labOrderRepository.findAll(pageable).map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository))
                : labOrderRepository.searchLabOrders(term, pageable).map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String email = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            if (statusFilter != null) {
                return labOrderRepository.findByDoctorEmailAndStatusAndSearch(email, statusFilter, term, pageable)
                        .map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
            }
            return labOrderRepository.findByDoctorEmailAndSearch(email, term, pageable)
                    .map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            if (statusFilter != null) {
                return labOrderRepository.findByPatientEmailAndStatusAndSearch(email, statusFilter, term, pageable)
                        .map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
            }
            return labOrderRepository.findByPatientEmailAndSearch(email, term, pageable)
                    .map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
        }

        throw new AccessDeniedException("Access denied.");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderResponseDTO> getLabOrdersByPatient(String patientCode, int page, int size) {
        patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));
        validatePatientReadAccess(patientCode);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return labOrderRepository.findByPatient_PatientCode(patientCode, pageable)
                .map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabOrderResponseDTO> getLabOrdersByDoctor(String doctorCode, int page, int size) {
        doctorRepository.findByDoctorCode(doctorCode)
                .orElseThrow(() -> new DoctorNotFoundException(doctorCode));
        validateDoctorReadAccess(doctorCode);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return labOrderRepository.findByDoctor_DoctorCode(doctorCode, pageable)
                .map(o -> LabOrderMapper.toResponseDTO(o, labReportRepository));
    }

    // ── Status Update ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabOrderResponseDTO updateLabOrderStatus(String labOrderCode, LabOrderStatusUpdateDTO request) {
        LabOrder order = labOrderRepository.findByLabOrderCode(labOrderCode)
                .orElseThrow(() -> new LabOrderNotFoundException(labOrderCode));

        validateDoctorOwnership(order.getConsultation());
        validateStatusTransition(order.getStatus(), request.getStatus());

        order.setStatus(request.getStatus());
        LabOrder saved = labOrderRepository.save(order);

        log.info("Lab order status updated | code={}, status={}", labOrderCode, request.getStatus());
        return LabOrderMapper.toResponseDTO(saved, labReportRepository);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void validateNoDuplicateTests(List<LabOrderItemRequestDTO> items) {
        Set<String> seen = new HashSet<>();
        for (LabOrderItemRequestDTO item : items) {
            String normalized = item.getTestName().trim().toLowerCase();
            if (!seen.add(normalized)) {
                throw new BusinessRuleViolationException(
                    "Duplicate test '" + item.getTestName() + "' found in the same order. Each test must appear only once.");
            }
        }
    }

    private void validateStatusTransition(
            com.mediflow.platform.lab.enums.LabOrderStatus current,
            com.mediflow.platform.lab.enums.LabOrderStatus next) {

        if (current == com.mediflow.platform.lab.enums.LabOrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                "Cannot update a CANCELLED lab order.");
        }
        if (current == com.mediflow.platform.lab.enums.LabOrderStatus.COMPLETED
                && next != com.mediflow.platform.lab.enums.LabOrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                "A COMPLETED lab order can only be CANCELLED.");
        }
    }

    // ── Security Helpers ──────────────────────────────────────────────────────

    private void validateDoctorOwnership(Consultation consultation) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (!isDoctor) throw new AccessDeniedException("Only doctors and administrators can manage lab orders.");

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();
        if (consultation.getDoctor().getUser() == null ||
                !loggedInEmail.equals(consultation.getDoctor().getUser().getEmail())) {
            log.warn("[Security] Lab order ownership violation | user={}, consultation={}",
                    loggedInEmail, consultation.getConsultationCode());
            throw new AccessDeniedException("Access denied. You can only manage lab orders for your own patients.");
        }
    }

    private void validateReadAccess(LabOrder order) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            if (order.getDoctor().getUser() == null ||
                    !loggedInEmail.equals(order.getDoctor().getUser().getEmail())) {
                throw new AccessDeniedException("Access denied. You can only view your own lab orders.");
            }
            return;
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            if (order.getPatient().getUser() == null ||
                    !loggedInEmail.equals(order.getPatient().getUser().getEmail())) {
                log.warn("[Security] Patient lab order access violation | user={}, order={}",
                        loggedInEmail, order.getLabOrderCode());
                throw new AccessDeniedException("Access denied. You can only view your own lab orders.");
            }
        }
    }

    private void validatePatientReadAccess(String patientCode) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;
        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) return;

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String loggedInEmail = principal.getEmail();
            var patient = patientRepository.findByPatientCode(patientCode)
                    .orElseThrow(() -> new PatientNotFoundException(patientCode));
            if (patient.getUser() == null || !loggedInEmail.equals(patient.getUser().getEmail())) {
                throw new AccessDeniedException("Access denied. You can only view your own lab orders.");
            }
        }
    }

    private void validateDoctorReadAccess(String doctorCode) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;
        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String loggedInEmail = principal.getEmail();
            Optional<com.mediflow.platform.doctor.entity.Doctor> callerDoctor =
                    doctorRepository.findByUser_Email(loggedInEmail);
            if (callerDoctor.isEmpty() || !doctorCode.equals(callerDoctor.get().getDoctorCode())) {
                throw new AccessDeniedException("Access denied. You can only view your own lab orders.");
            }
        }
    }

    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required.");
        }
        return auth;
    }

    // ── Code Generation ───────────────────────────────────────────────────────

    /**
     * Year-scoped sequential lab order code.
     * Format: LAB-YYYY-NNNN (e.g. LAB-2026-0001)
     */
    private String generateLabOrderCode() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "LAB-" + year + "-";
        Optional<LabOrder> latest = labOrderRepository
                .findTopByLabOrderCodeStartingWithOrderByLabOrderCodeDesc(prefix);
        int nextSequence = 1;
        if (latest.isPresent()) {
            String lastCode = latest.get().getLabOrderCode();
            nextSequence = Integer.parseInt(lastCode.substring(lastCode.lastIndexOf('-') + 1)) + 1;
        }
        if (nextSequence > 9999) {
            throw new IllegalStateException(
                "Lab order code sequence limit (9999) reached for year " + year);
        }
        return prefix + String.format("%04d", nextSequence);
    }
}
