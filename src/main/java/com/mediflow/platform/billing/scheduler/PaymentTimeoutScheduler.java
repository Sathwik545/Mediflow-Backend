package com.mediflow.platform.billing.scheduler;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import com.mediflow.platform.appointment.repository.AppointmentRepository;
import com.mediflow.platform.billing.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that automatically cancels PAYMENT_PENDING appointments
 * whose payment window has expired.
 *
 * Payment window: 15 minutes from appointment creation.
 * Scan frequency: every 60 seconds (fixedDelay — starts after previous run completes).
 *
 * Each appointment is processed in its own transaction via BillService.expirePaymentPendingAppointment().
 * A status double-check inside that transaction guards against the race condition where
 * a payment arrives between this scheduler's DB scan and the actual cancellation.
 *
 * On cancellation: AppointmentStatus → CANCELLED, BillStatus → CANCELLED.
 * Doctor's slot is immediately freed for new bookings.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final AppointmentRepository appointmentRepository;
    private final BillService billService;

    @Scheduled(fixedDelay = 60_000)
    public void expireStalePaymentPendingAppointments() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        List<Appointment> expired = appointmentRepository
                .findByAppointmentStatusAndCreatedAtBefore(
                        AppointmentStatus.PAYMENT_PENDING,
                        expiryThreshold
                );

        if (expired.isEmpty()) {
            return;
        }

        log.info("Payment timeout scheduler: {} expired PAYMENT_PENDING appointment(s) found",
                expired.size());

        int cancelled = 0;
        for (Appointment appointment : expired) {
            try {
                billService.expirePaymentPendingAppointment(appointment.getId());
                cancelled++;
            } catch (Exception ex) {
                log.error("Failed to expire appointment | id={}, code={} | error={}",
                        appointment.getId(), appointment.getAppointmentCode(), ex.getMessage());
            }
        }

        if (cancelled > 0) {
            log.info("Payment timeout scheduler: auto-cancelled {} appointment(s)", cancelled);
        }
    }
}
