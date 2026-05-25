package com.mediflow.platform.billing.invoice.exception;

import com.mediflow.platform.common.exception.BusinessRuleViolationException;

/**
 * Thrown when an invoice PDF is requested for a bill that is not in PAID status.
 * Maps to HTTP 422 via GlobalExceptionHandler → BusinessRuleViolationException handler.
 *
 * Business rule: invoices are only available for bills with paymentStatus = PAID.
 * PENDING and CANCELLED bills have no invoice.
 */
public class InvoiceNotAvailableException extends BusinessRuleViolationException {

    public InvoiceNotAvailableException(String billCode, Object currentStatus) {
        super("Invoice is not available for bill '" + billCode + "'. " +
              "Current status: " + currentStatus + ". " +
              "Invoices can only be generated for PAID bills.");
    }
}
