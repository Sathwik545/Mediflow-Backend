package com.mediflow.platform.lab.repository;

import com.mediflow.platform.lab.entity.LabReport;
import com.mediflow.platform.lab.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabReportRepository extends JpaRepository<LabReport, Long> {

    Optional<LabReport> findByReportCode(String reportCode);

    /** Used for year-scoped sequential code generation: REP-YYYY-NNNN */
    Optional<LabReport> findTopByReportCodeStartingWithOrderByReportCodeDesc(String prefix);

    /** Prevents duplicate reports for the same lab order item */
    Optional<LabReport> findByLabOrderItem_Id(Long labOrderItemId);

    boolean existsByLabOrderItem_Id(Long labOrderItemId);

    List<LabReport> findByLabOrder_LabOrderCode(String labOrderCode);

    Page<LabReport> findByPatient_PatientCode(String patientCode, Pageable pageable);

    Page<LabReport> findByReportStatus(ReportStatus status, Pageable pageable);

    @Query("""
        SELECT r FROM LabReport r
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(r.reportCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.labOrder.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabReport> searchLabReports(@Param("search") String search, Pageable pageable);

    /** Doctor-scoped search — returns only reports belonging to the doctor with the given email. */
    @Query("""
        SELECT r FROM LabReport r
        WHERE r.doctor.user.email = :email
          AND (:search IS NULL OR :search = ''
               OR LOWER(r.reportCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.labOrder.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabReport> findByDoctorEmailAndSearch(
        @Param("email") String email,
        @Param("search") String search,
        Pageable pageable);

    /** Patient-scoped search — returns only VERIFIED reports for the patient with the given email. */
    @Query("""
        SELECT r FROM LabReport r
        WHERE r.patient.user.email = :email
          AND r.reportStatus = com.mediflow.platform.lab.enums.ReportStatus.VERIFIED
          AND (:search IS NULL OR :search = ''
               OR LOWER(r.reportCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.labOrder.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabReport> findVerifiedByPatientEmailAndSearch(
        @Param("email") String email,
        @Param("search") String search,
        Pageable pageable);

    long countByReportStatus(ReportStatus status);
}
