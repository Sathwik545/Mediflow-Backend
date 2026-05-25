package com.mediflow.platform.lab.repository;

import com.mediflow.platform.lab.entity.LabOrder;
import com.mediflow.platform.lab.enums.LabOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    Optional<LabOrder> findByLabOrderCode(String labOrderCode);

    boolean existsByLabOrderCode(String labOrderCode);

    /** Used for year-scoped sequential code generation: LAB-YYYY-NNNN */
    Optional<LabOrder> findTopByLabOrderCodeStartingWithOrderByLabOrderCodeDesc(String prefix);

    Page<LabOrder> findByPatient_PatientCode(String patientCode, Pageable pageable);

    Page<LabOrder> findByDoctor_DoctorCode(String doctorCode, Pageable pageable);

    Page<LabOrder> findByStatus(LabOrderStatus status, Pageable pageable);

    Page<LabOrder> findByPatient_PatientCodeAndStatus(String patientCode, LabOrderStatus status, Pageable pageable);

    @Query("""
        SELECT o FROM LabOrder o
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(o.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabOrder> searchLabOrders(@Param("search") String search, Pageable pageable);

    /** Doctor-scoped search — returns only orders belonging to the doctor with the given email. */
    @Query("""
        SELECT o FROM LabOrder o
        WHERE o.doctor.user.email = :email
          AND (:search IS NULL OR :search = ''
               OR LOWER(o.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabOrder> findByDoctorEmailAndSearch(
        @Param("email") String email,
        @Param("search") String search,
        Pageable pageable);

    /** Patient-scoped search — returns only orders belonging to the patient with the given email. */
    @Query("""
        SELECT o FROM LabOrder o
        WHERE o.patient.user.email = :email
          AND (:search IS NULL OR :search = ''
               OR LOWER(o.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabOrder> findByPatientEmailAndSearch(
        @Param("email") String email,
        @Param("search") String search,
        Pageable pageable);

    long countByStatus(LabOrderStatus status);

    /** Admin search filtered by status. */
    @Query("""
        SELECT o FROM LabOrder o
        WHERE o.status = :status
          AND (:search IS NULL OR :search = ''
               OR LOWER(o.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabOrder> findByStatusAndSearch(
        @Param("status") LabOrderStatus status,
        @Param("search") String search,
        Pageable pageable);

    /** Doctor-scoped search filtered by status. */
    @Query("""
        SELECT o FROM LabOrder o
        WHERE o.doctor.user.email = :email
          AND o.status = :status
          AND (:search IS NULL OR :search = ''
               OR LOWER(o.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabOrder> findByDoctorEmailAndStatusAndSearch(
        @Param("email") String email,
        @Param("status") LabOrderStatus status,
        @Param("search") String search,
        Pageable pageable);

    /** Patient-scoped search filtered by status. */
    @Query("""
        SELECT o FROM LabOrder o
        WHERE o.patient.user.email = :email
          AND o.status = :status
          AND (:search IS NULL OR :search = ''
               OR LOWER(o.labOrderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.patient.patientCode) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<LabOrder> findByPatientEmailAndStatusAndSearch(
        @Param("email") String email,
        @Param("status") LabOrderStatus status,
        @Param("search") String search,
        Pageable pageable);
}
