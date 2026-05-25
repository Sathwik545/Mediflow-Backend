package com.mediflow.platform.billing.invoice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Snapshot-based invoice data transfer object.
 *
 * All fields are populated at invoice generation time:
 *  — Organization fields come from HospitalSettings (the centralized config source).
 *  — Patient/doctor name fields come from Bill snapshot columns (immutable from booking time).
 *  — Payment fields come from the Bill entity at the moment of PDF generation.
 *
 * This DTO is never persisted — it is assembled on-the-fly each time a PDF is requested.
 * Historical accuracy is preserved because the Bill entity stores name and fee snapshots.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceReceiptDTO {

    // ── Organization (from HospitalSettings — never hardcoded) ───────────────
    private String hospitalName;
    private String hospitalCode;
    private String hospitalPhone;
    private String hospitalEmail;
    private String supportEmail;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String gstNumber;
    private String currencyCode;
    private String timezone;
    private String logoUrl;

    // ── Invoice metadata ─────────────────────────────────────────────────────
    private String invoiceNumber;      // INV-YYYY-NNNN  (derived from billCode)
    private String billCode;
    private String appointmentCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate generatedDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paidDate;

    // ── Patient (name from Bill snapshot; phone from live Patient entity) ─────
    private String patientName;        // immutable snapshot from bill
    private String patientCode;
    private String patientPhone;

    // ── Doctor (name from Bill snapshot; dept/spec from live Doctor entity) ───
    private String doctorName;         // immutable snapshot from bill
    private String department;
    private String specialization;

    // ── Appointment ──────────────────────────────────────────────────────────
    private String consultationType;   // IN_PERSON, ONLINE, FOLLOW_UP (human-readable)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;
    private String appointmentTime;    // e.g. "10:00 - 10:30"

    // ── Payment ──────────────────────────────────────────────────────────────
    private BigDecimal consultationFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;      // human-readable: "Cash", "UPI", etc.
    private String paymentStatus;

    // ── Audit ────────────────────────────────────────────────────────────────
    private String generatedBy;        // email of user who created the bill

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;
}
