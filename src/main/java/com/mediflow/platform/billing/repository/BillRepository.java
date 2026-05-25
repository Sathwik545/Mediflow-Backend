package com.mediflow.platform.billing.repository;

import com.mediflow.platform.billing.entity.Bill;
import com.mediflow.platform.billing.enums.BillStatus;
import com.mediflow.platform.billing.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillCode(String billCode);

    // Used by bill code generation — mirrors AppointmentRepository.findTopByAppointmentCode pattern
    Optional<Bill> findTopByBillCodeStartingWithOrderByBillCodeDesc(String prefix);

    Page<Bill> findByPatient_PatientCode(String patientCode, Pageable pageable);

    Optional<Bill> findByAppointment_AppointmentCode(String appointmentCode);

    /**
     * Server-side search + filter for the billing dashboard.
     *
     * search   — OR-matched case-insensitively against billCode, appointmentCode, patientCode;
     *            pass null (or empty) to skip full-text filtering.
     * paymentStatus — filters by PaymentStatus enum; pass null to skip.
     * billStatus    — filters by BillStatus enum (used for CANCELLED tab); pass null to skip.
     */
    @Query("SELECT b FROM Bill b WHERE " +
           "(COALESCE(:search, '') = '' OR " +
           " LOWER(b.billCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(b.appointment.appointmentCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(b.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:paymentStatus IS NULL OR b.paymentStatus = :paymentStatus) " +
           "AND (:billStatus IS NULL OR b.billStatus = :billStatus)")
    Page<Bill> searchBills(
            @Param("search") String search,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("billStatus") BillStatus billStatus,
            Pageable pageable);
}
