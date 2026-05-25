package com.mediflow.platform.billing.invoice.service;

/**
 * Contract for invoice PDF generation.
 *
 * Invoices are dynamically generated from Bill snapshot data and HospitalSettings.
 * No InvoiceEntity exists — the Bill entity is the financial source of truth.
 *
 * Access control is enforced inside the implementation:
 *   ADMIN  — can generate invoices for any bill
 *   PATIENT — can only generate invoices for their own bills
 *   DOCTOR  — no access (AccessDeniedException → 403)
 *
 * Business rule: only PAID bills produce an invoice.
 * PENDING / CANCELLED bills throw InvoiceNotAvailableException → 422.
 */
public interface InvoiceService {

    /**
     * Validates bill eligibility, enforces ownership, assembles snapshot data
     * from HospitalSettings + Bill, and delegates PDF rendering to InvoicePdfService.
     *
     * @param billCode  unique bill code (e.g. BILL-2026-0001)
     * @return          raw PDF bytes, ready to stream as application/pdf
     * @throws com.mediflow.platform.billing.exception.BillNotFoundException
     *         if no bill exists for the given code (404)
     * @throws com.mediflow.platform.billing.invoice.exception.InvoiceNotAvailableException
     *         if bill is not in PAID status (422)
     * @throws org.springframework.security.access.AccessDeniedException
     *         if the caller is not authorised to view this bill (403)
     */
    byte[] generateInvoicePdf(String billCode);
}
