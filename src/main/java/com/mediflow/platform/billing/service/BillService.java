package com.mediflow.platform.billing.service;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.billing.dto.BillResponseDTO;
import com.mediflow.platform.billing.dto.PayBillRequestDTO;
import org.springframework.data.domain.Page;

public interface BillService {

    /**
     * Generates a consultation bill for a newly booked appointment.
     * Called internally by AppointmentServiceImpl within the same transaction.
     */
    BillResponseDTO generateBillForAppointment(Appointment appointment);

    /**
     * Cancels the bill linked to the given appointment code.
     * Called when an appointment is cancelled before payment.
     */
    void cancelBillForAppointment(String appointmentCode);

    /**
     * Auto-cancels a PAYMENT_PENDING appointment and its bill when the payment window expires.
     * Called by PaymentTimeoutScheduler. Re-validates status inside the transaction to guard
     * against the race condition where payment arrives between the scheduler query and this call.
     */
    void expirePaymentPendingAppointment(Long appointmentId);

    BillResponseDTO getBillByCode(String billCode);

    /**
     * Returns bills with optional server-side search and status filter.
     * search  — matches billCode, appointmentCode, or patientCode (OR, case-insensitive); null = no filter.
     * status  — "PENDING"|"PAID" filter by paymentStatus; "CANCELLED" filter by billStatus; null/"ALL" = no filter.
     * Admin-only; used by the billing dashboard.
     */
    Page<BillResponseDTO> getAllBills(String search, String status, int page, int size);

    Page<BillResponseDTO> getBillsByPatient(String patientCode, int page, int size);

    /**
     * Marks a bill as PAID and transitions the linked appointment to CONFIRMED.
     * Validates that the bill exists, is not already paid, and the appointment is not cancelled.
     */
    BillResponseDTO payBill(String billCode, PayBillRequestDTO request);
}
