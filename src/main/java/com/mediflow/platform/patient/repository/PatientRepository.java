package com.mediflow.platform.patient.repository;

import com.mediflow.platform.patient.entity.Patient;
import com.mediflow.platform.patient.enums.PatientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientCode(String patientCode);

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByUser_Email(String email);

    boolean existsByEmail(String email);

    boolean existsByPatientCode(String patientCode);

    boolean existsByEmailAndPatientCodeNot(String email, String patientCode);

    long countByStatus(PatientStatus status);

    Page<Patient> findAllByStatus(PatientStatus status, Pageable pageable);

    @Query("""
        SELECT p FROM Patient p
        WHERE p.status = com.mediflow.platform.patient.enums.PatientStatus.ACTIVE
          AND (LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.patientCode) LIKE LOWER(CONCAT('%', :q, '%'))
            OR p.phoneNumber LIKE CONCAT('%', :q, '%')
            OR LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Patient> searchActivePatients(@Param("q") String q, Pageable pageable);
}
