package com.mediflow.platform.billing.invoice.service.impl;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.auth.security.UserPrincipal;
import com.mediflow.platform.billing.entity.Bill;
import com.mediflow.platform.billing.enums.BillStatus;
import com.mediflow.platform.billing.enums.PaymentStatus;
import com.mediflow.platform.billing.exception.BillNotFoundException;
import com.mediflow.platform.billing.invoice.dto.InvoiceReceiptDTO;
import com.mediflow.platform.billing.invoice.exception.InvoiceNotAvailableException;
import com.mediflow.platform.billing.invoice.pdf.InvoicePdfService;
import com.mediflow.platform.billing.invoice.service.InvoiceService;
import com.mediflow.platform.billing.repository.BillRepository;
import com.mediflow.platform.common.exception.BusinessRuleViolationException;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.patient.entity.Patient;
import com.mediflow.platform.patient.exception.PatientNotFoundException;
import com.mediflow.platform.patient.repository.PatientRepository;
import com.mediflow.platform.settings.dto.HospitalSettingsResponseDTO;
import com.mediflow.platform.settings.service.HospitalSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles invoice data from Bill snapshots + HospitalSettings and delegates
 * PDF rendering to InvoicePdfService.
 *
 * ── Access control (mirrors BillServiceImpl.validateBillOwnership) ─────────
 *  ADMIN   — unrestricted access to all invoices
 *  PATIENT — own bills only (validated via JWT email ↔ Patient.user.email)
 *  DOCTOR  — denied (403)
 *  Anon    — denied (403)
 *
 * ── Business rules ──────────────────────────────────────────────────────────
 *  Only PAID bills produce an invoice.
 *  CANCELLED and PENDING bills throw InvoiceNotAvailableException (422).
 *
 * ── Snapshot fidelity ───────────────────────────────────────────────────────
 *  Patient/doctor names come from Bill.patientNameSnapshot / doctorNameSnapshot —
 *  frozen at booking time, never affected by profile changes.
 *  Specialization and department are loaded from the live Doctor entity because
 *  no snapshot exists for them; they change far less frequently than names.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final BillRepository           billRepository;
    private final PatientRepository        patientRepository;
    private final HospitalSettingsService  hospitalSettingsService;
    private final InvoicePdfService        invoicePdfService;

    /**
     * Full invoice lifecycle:
     *  1. Load bill (with lazy associations — within @Transactional session)
     *  2. Validate access control (same pattern as BillServiceImpl)
     *  3. Enforce PAID-only business rule
     *  4. Load hospital settings (centralized config — never hardcoded)
     *  5. Assemble InvoiceReceiptDTO from snapshots + live entities + settings
     *  6. Delegate PDF rendering to InvoicePdfService
     *  7. Log access for audit trail
     *  8. Return raw PDF bytes
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(String billCode) {
        Bill bill = billRepository.findByBillCode(billCode)
                .orElseThrow(() -> new BillNotFoundException(billCode));

        // ── Access control ────────────────────────────────────────────────────
        validateInvoiceAccess(bill.getPatient().getPatientCode());

        // ── Business rule: PAID bills only ────────────────────────────────────
        if (bill.getPaymentStatus() != PaymentStatus.PAID) {
            throw new InvoiceNotAvailableException(billCode, bill.getPaymentStatus());
        }
        if (bill.getBillStatus() == BillStatus.CANCELLED) {
            throw new InvoiceNotAvailableException(billCode, "CANCELLED");
        }

        // ── Load organization config — single-row settings table ──────────────
        HospitalSettingsResponseDTO settings = hospitalSettingsService.getSettings();
        if (settings == null) {
            throw new BusinessRuleViolationException(
                "Hospital settings are not configured. Please configure them before generating invoices.");
        }

        // ── Assemble InvoiceReceiptDTO ────────────────────────────────────────
        InvoiceReceiptDTO dto = buildInvoiceDto(bill, settings);

        // ── Generate PDF in-memory ────────────────────────────────────────────
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(dto);

        log.info("[Invoice] PDF generated | invoice={}, bill={}, patient={}, requestedBy={}",
                dto.getInvoiceNumber(), billCode,
                bill.getPatient().getPatientCode(),
                currentUserEmail());

        return pdfBytes;
    }

    // ── DTO assembly ─────────────────────────────────────────────────────────

    private InvoiceReceiptDTO buildInvoiceDto(Bill bill, HospitalSettingsResponseDTO s) {
        Appointment apt     = bill.getAppointment();
        Doctor      doctor  = bill.getDoctor();
        Patient     patient = bill.getPatient();

        // Invoice number: replace "BILL-" prefix with "INV-"  →  INV-YYYY-NNNN
        String invoiceNumber = "INV-" + bill.getBillCode().substring(5);

        // Time slot: "HH:mm - HH:mm"
        String timeSlot = apt.getStartTime() + " - " + apt.getEndTime();

        // Payment method: enum name to human-readable string
        String paymentMethod = bill.getPaymentMethod() != null
                ? bill.getPaymentMethod().name()
                : null;

        return InvoiceReceiptDTO.builder()
                // ── Organization (from HospitalSettings — no hardcoded values) ─
                .hospitalName(s.getHospitalName())
                .hospitalCode(s.getHospitalCode())
                .hospitalPhone(s.getPhoneNumber())
                .hospitalEmail(s.getEmail())
                .supportEmail(s.getSupportEmail())
                .addressLine1(s.getAddressLine1())
                .addressLine2(s.getAddressLine2())
                .city(s.getCity())
                .state(s.getState())
                .postalCode(s.getPostalCode())
                .country(s.getCountry())
                .gstNumber(s.getGstNumber())
                .currencyCode(s.getCurrencyCode())
                .timezone(s.getTimezone())
                .logoUrl(s.getLogoUrl())
                // ── Invoice metadata ────────────────────────────────────────────
                .invoiceNumber(invoiceNumber)
                .billCode(bill.getBillCode())
                .appointmentCode(apt.getAppointmentCode())
                .generatedDate(bill.getGeneratedAt().toLocalDate())
                .paidDate(bill.getPaidAt())
                // ── Patient (name from immutable snapshot; phone from live entity)
                .patientName(bill.getPatientNameSnapshot())
                .patientCode(patient.getPatientCode())
                .patientPhone(patient.getPhoneNumber())
                // ── Doctor (name from immutable snapshot; dept/spec from live entity)
                .doctorName(bill.getDoctorNameSnapshot())
                .department(doctor.getDepartment())
                .specialization(doctor.getSpecialization())
                // ── Appointment ─────────────────────────────────────────────────
                .consultationType(apt.getConsultationType().name())
                .appointmentDate(apt.getAppointmentDate())
                .appointmentTime(timeSlot)
                // ── Payment ─────────────────────────────────────────────────────
                .consultationFee(bill.getConsultationFeeSnapshot())
                .taxAmount(bill.getTaxAmount())
                .discountAmount(bill.getDiscountAmount())
                .totalAmount(bill.getTotalAmount())
                .paymentMethod(paymentMethod)
                .paymentStatus(bill.getPaymentStatus().name())
                // ── Audit ────────────────────────────────────────────────────────
                .generatedBy(bill.getCreatedBy())
                .generatedAt(bill.getGeneratedAt())
                .build();
    }

    // ── Access control ────────────────────────────────────────────────────────

    /**
     * Enforces invoice access control using the same pattern as BillServiceImpl:
     *  ADMIN   — unrestricted
     *  PATIENT — can only access invoices whose patient code belongs to them (JWT email match)
     *  DOCTOR / others — denied (403)
     *
     * Never trusts patientCode from the request — always reads from the bill entity
     * and validates against the JWT SecurityContext.
     */
    private void validateInvoiceAccess(String patientCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required to access invoice records.");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return; // ADMINs have unrestricted invoice access
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));

        if (!isPatient) {
            log.warn("[Invoice][Security] Forbidden access | role={}", auth.getAuthorities());
            throw new AccessDeniedException(
                "Access denied. Only patients and administrators can access invoice records.");
        }

        // PATIENT: verify that the bill's patient code belongs to the logged-in user
        UserPrincipal principal   = (UserPrincipal) auth.getPrincipal();
        String        loggedEmail = principal.getEmail();

        Patient patient = patientRepository.findByPatientCode(patientCode)
                .orElseThrow(() -> new PatientNotFoundException(patientCode));

        if (patient.getUser() == null || !loggedEmail.equals(patient.getUser().getEmail())) {
            log.warn("[Invoice][Security] Forbidden access | user={} attempted invoice for patient={}",
                    loggedEmail, patientCode);
            throw new AccessDeniedException(
                "Access denied. You can only view invoices for your own appointments.");
        }
    }

    private String currentUserEmail() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
                return up.getEmail();
            }
        } catch (Exception ignored) { }
        return "unknown";
    }
}
