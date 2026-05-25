package com.mediflow.platform.billing.service.impl;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.repository.AppointmentRepository;
import com.mediflow.platform.auth.security.UserPrincipal;
import com.mediflow.platform.billing.dto.BillResponseDTO;
import com.mediflow.platform.billing.dto.PayBillRequestDTO;
import com.mediflow.platform.billing.entity.Bill;
import com.mediflow.platform.billing.enums.BillStatus;
import com.mediflow.platform.billing.enums.BillType;
import com.mediflow.platform.billing.enums.PaymentStatus;
import com.mediflow.platform.billing.exception.BillNotFoundException;
import com.mediflow.platform.billing.mapper.BillMapper;
import com.mediflow.platform.billing.repository.BillRepository;
import com.mediflow.platform.billing.service.BillService;
import com.mediflow.platform.common.exception.BusinessRuleViolationException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    /**
     * Generates a consultation bill immediately after appointment creation.
     *
     * Snapshot values frozen at generation time:
     *  - consultationFeeSnapshot: from appointment.consultationFeeSnapshot (already frozen from Doctor.consultationFee)
     *  - patientNameSnapshot: from appointment.patientNameSnapshot (frozen at booking)
     *  - doctorNameSnapshot: from appointment.doctorNameSnapshot (frozen at booking)
     *
     * Historical financial records remain immutable even if names or fees change later.
     * Runs within the caller's @Transactional (REQUIRED propagation).
     */
    @Override
    @Transactional
    public BillResponseDTO generateBillForAppointment(Appointment appointment) {
        String billCode = generateBillCode();
        BigDecimal fee      = appointment.getConsultationFeeSnapshot();
        BigDecimal tax      = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total    = fee.add(tax).subtract(discount);

        Bill bill = Bill.builder()
                .billCode(billCode)
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .billType(BillType.CONSULTATION)
                .consultationFeeSnapshot(fee)
                .patientNameSnapshot(appointment.getPatientNameSnapshot())
                .doctorNameSnapshot(appointment.getDoctorNameSnapshot())
                .taxAmount(tax)
                .discountAmount(discount)
                .totalAmount(total)
                .paymentStatus(PaymentStatus.PENDING)
                .billStatus(BillStatus.GENERATED)
                .generatedAt(LocalDateTime.now())
                .build();

        Bill saved = billRepository.save(bill);

        log.info("Bill generated | code={}, appointment={}, fee={}",
                billCode, appointment.getAppointmentCode(), fee);

        return BillMapper.toResponseDTO(saved);
    }

    /**
     * Marks the bill linked to the given appointment as CANCELLED.
     * Called when a PAYMENT_PENDING appointment is cancelled (before payment).
     * No-op if no bill is found (defensive guard).
     */
    @Override
    @Transactional
    public void cancelBillForAppointment(String appointmentCode) {
        billRepository.findByAppointment_AppointmentCode(appointmentCode).ifPresent(bill -> {
            bill.setBillStatus(BillStatus.CANCELLED);
            billRepository.save(bill);
            log.info("Bill cancelled | code={}, appointment={}", bill.getBillCode(), appointmentCode);
        });
    }

    /**
     * Auto-cancels a PAYMENT_PENDING appointment and its bill when the payment window expires.
     *
     * Re-validates status inside this transaction to guard against the race condition where
     * a payment arrives between the scheduler's DB scan and this execution.
     * If the appointment is no longer PAYMENT_PENDING (e.g., was just paid), this is a no-op.
     */
    @Override
    @Transactional
    public void expirePaymentPendingAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return;
        }
        if (appointment.getAppointmentStatus() != AppointmentStatus.PAYMENT_PENDING) {
            log.debug("Skipping expiry for appointment id={} — status is already {}",
                    appointmentId, appointment.getAppointmentStatus());
            return;
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        billRepository.findByAppointment_AppointmentCode(appointment.getAppointmentCode())
                .ifPresent(bill -> {
                    bill.setBillStatus(BillStatus.CANCELLED);
                    billRepository.save(bill);
                });

        log.info("Payment timeout: auto-cancelled | appointment={}, patient={}",
                appointment.getAppointmentCode(),
                appointment.getPatient().getPatientCode());
    }

    /**
     * Returns bills with optional server-side search and status filter.
     *
     * search maps to an OR query across billCode, appointmentCode, patientCode (case-insensitive).
     * status "PENDING"/"PAID" → paymentStatus filter; "CANCELLED" → billStatus filter; null/"ALL" → no filter.
     * Authorization enforced at the controller layer via @PreAuthorize("hasRole('ADMIN')").
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BillResponseDTO> getAllBills(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"));

        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        PaymentStatus paymentStatus = null;
        BillStatus billStatus = null;
        if (status != null) {
            switch (status.toUpperCase()) {
                case "PENDING"   -> paymentStatus = PaymentStatus.PENDING;
                case "PAID"      -> paymentStatus = PaymentStatus.PAID;
                case "CANCELLED" -> billStatus    = BillStatus.CANCELLED;
                default          -> { /* ALL or unknown — no filter */ }
            }
        }

        return billRepository.searchBills(trimmedSearch, paymentStatus, billStatus, pageable)
                .map(BillMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponseDTO getBillByCode(String billCode) {
        Bill bill = billRepository.findByBillCode(billCode)
                .orElseThrow(() -> new BillNotFoundException(billCode));

        validateBillOwnership(bill.getPatient().getPatientCode());

        return BillMapper.toResponseDTO(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BillResponseDTO> getBillsByPatient(String patientCode, int page, int size) {
        validateBillOwnership(patientCode);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "generatedAt"));
        return billRepository.findByPatient_PatientCode(patientCode, pageable)
                .map(BillMapper::toResponseDTO);
    }

    /**
     * Records a manual payment against a bill.
     *
     * Transition rules:
     *  - Bill: PENDING → PAID; paidAt set to now; paymentMethod recorded.
     *  - Appointment: PAYMENT_PENDING → CONFIRMED.
     *
     * Both updates occur in the same transaction — either both succeed or both roll back.
     */
    @Override
    @Transactional
    public BillResponseDTO payBill(String billCode, PayBillRequestDTO request) {
        Bill bill = billRepository.findByBillCode(billCode)
                .orElseThrow(() -> new BillNotFoundException(billCode));

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleViolationException(
                "Bill " + billCode + " has already been paid."
            );
        }

        Appointment appointment = bill.getAppointment();

        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                "Cannot process payment for a cancelled appointment."
            );
        }

        bill.setPaymentStatus(PaymentStatus.PAID);
        bill.setPaymentMethod(request.getPaymentMethod());
        bill.setPaidAt(LocalDateTime.now());

        appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        Bill saved = billRepository.save(bill);

        log.info("Bill paid | code={}, method={}, appointment={}",
                billCode, request.getPaymentMethod(), appointment.getAppointmentCode());

        return BillMapper.toResponseDTO(saved);
    }

    // ── Ownership Validation ────────────────────────────────────────────────────

    /**
     * Enforces bill access control:
     *  ADMIN  — full access to all bills.
     *  PATIENT — can only access bills whose patient code matches their own.
     *  DOCTOR / others — denied entirely.
     *
     * Uses the JWT principal from SecurityContext; never trusts request parameters.
     * Throws AccessDeniedException (→ HTTP 403) on any violation.
     */
    private void validateBillOwnership(String patientCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required to access bill records.");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return; // ADMINs have unrestricted access
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));

        if (!isPatient) {
            log.warn("[Security] Forbidden bill access | role={}", auth.getAuthorities());
            throw new AccessDeniedException(
                "Access denied. Only patients and administrators can access bill records."
            );
        }

        // PATIENT role: verify the requested patient code belongs to the logged-in user
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        Patient patient = patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));

        if (patient.getUser() == null || !loggedInEmail.equals(patient.getUser().getEmail())) {
            log.warn("[Security] Forbidden bill access | user={} attempted to access bills of patient={}",
                    loggedInEmail, patientCode);
            throw new AccessDeniedException(
                "Access denied. You can only view your own bill records."
            );
        }
    }

    // ── Bill Code Generation ────────────────────────────────────────────────────

    /**
     * Generates a year-scoped sequential bill code.
     *
     * Format : BILL-YYYY-NNNN
     * Example: BILL-2026-0001, BILL-2026-0042
     *
     * Mirrors the DoctorServiceImpl.generateDoctorCode() strategy exactly.
     */
    private String generateBillCode() {
        String year   = String.valueOf(LocalDate.now().getYear());
        String prefix = "BILL-" + year + "-";

        Optional<Bill> latest = billRepository
                .findTopByBillCodeStartingWithOrderByBillCodeDesc(prefix);

        int nextSequence = 1;
        if (latest.isPresent()) {
            String lastCode     = latest.get().getBillCode();
            String sequencePart = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(sequencePart) + 1;
        }

        if (nextSequence > 9999) {
            throw new IllegalStateException(
                "Bill code sequence limit (9999) reached for year " + year +
                ". Contact the system administrator to expand the format."
            );
        }

        return prefix + String.format("%04d", nextSequence);
    }
}
