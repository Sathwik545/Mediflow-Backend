package com.mediflow.platform.billing.invoice.controller;

import com.mediflow.platform.billing.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Invoice PDF controller — preview and download endpoints.
 *
 * Both endpoints produce the same PDF bytes; they differ only in Content-Disposition:
 *   Preview  → inline   (browser opens PDF in-tab / built-in viewer)
 *   Download → attachment (browser prompts Save-As)
 *
 * ── Access control (enforced in InvoiceServiceImpl) ────────────────────────
 *  ADMIN   — all invoices
 *  PATIENT — own invoices only
 *  DOCTOR  — 403 Forbidden (enforced at URL level in SecurityConfig + service layer)
 *
 * ── Business rule ───────────────────────────────────────────────────────────
 *  Only PAID bills return a PDF.
 *  PENDING / CANCELLED bills return 422 (InvoiceNotAvailableException).
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Management", description = "APIs for previewing and downloading consultation invoice PDFs")
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ── Preview (inline) ──────────────────────────────────────────────────────

    @Operation(
        summary = "Preview invoice PDF (inline)",
        description = "Streams the invoice PDF for the given bill code with Content-Disposition: inline, " +
                      "so the browser opens it in the built-in PDF viewer. " +
                      "Only available for PAID bills. " +
                      "ADMIN: any bill. PATIENT: own bills only. DOCTOR: 403."
    )
    @GetMapping(value = "/{billCode}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewInvoice(@PathVariable String billCode) {
        byte[] pdf = invoiceService.generateInvoicePdf(billCode);

        String invoiceNumber = deriveInvoiceNumber(billCode);
        String filename      = "Invoice_" + invoiceNumber + ".pdf";

        log.info("[Invoice] Preview | bill={}, invoice={}", billCode, invoiceNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    // ── Download (attachment) ─────────────────────────────────────────────────

    @Operation(
        summary = "Download invoice PDF (attachment)",
        description = "Streams the invoice PDF for the given bill code with Content-Disposition: attachment, " +
                      "prompting the browser to save the file. " +
                      "Only available for PAID bills. " +
                      "ADMIN: any bill. PATIENT: own bills only. DOCTOR: 403."
    )
    @GetMapping(value = "/{billCode}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String billCode) {
        byte[] pdf = invoiceService.generateInvoicePdf(billCode);

        String invoiceNumber = deriveInvoiceNumber(billCode);
        String filename      = "Invoice_" + invoiceNumber + ".pdf";

        log.info("[Invoice] Download | bill={}, invoice={}", billCode, invoiceNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Derives the invoice number from the bill code by replacing the "BILL-" prefix
     * with "INV-".  Example: BILL-2026-0001 → INV-2026-0001.
     *
     * The mapping is 1-to-1 — every bill has exactly one invoice number.
     */
    private String deriveInvoiceNumber(String billCode) {
        if (billCode != null && billCode.toUpperCase().startsWith("BILL-")) {
            return "INV-" + billCode.substring(5);
        }
        return "INV-" + billCode;
    }
}
