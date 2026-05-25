package com.mediflow.platform.doctor.repository;

import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByDoctorCode(String doctorCode);

    Optional<Doctor> findByEmail(String email);

    /** Used by ConsultationServiceImpl to resolve the currently logged-in doctor from JWT email. */
    Optional<Doctor> findByUser_Email(String email);

    boolean existsByEmail(String email);

    boolean existsByDoctorCode(String doctorCode);

    boolean existsByLicenseNumber(String licenseNumber);

    /** Used during update to check email uniqueness while excluding the doctor being updated. */
    boolean existsByEmailAndDoctorCodeNot(String email, String doctorCode);

    /** Used during update to check license uniqueness while excluding the doctor being updated. */
    boolean existsByLicenseNumberAndDoctorCodeNot(String licenseNumber, String doctorCode);

    long countByStatus(DoctorStatus status);

    Page<Doctor> findAllByStatus(DoctorStatus status, Pageable pageable);

    Page<Doctor> findAllByDepartmentIgnoreCase(String department, Pageable pageable);

    Page<Doctor> findAllBySpecializationIgnoreCase(String specialization, Pageable pageable);

    /**
     * Finds the most recently assigned doctor code for a given year prefix.
     * Used by the sequential code generator to determine the next sequence number.
     *
     * Example: prefix = "DOC-2026-" → returns doctor with code "DOC-2026-0042"
     *          so the next code generated will be "DOC-2026-0043".
     */
    Optional<Doctor> findTopByDoctorCodeStartingWithOrderByDoctorCodeDesc(String prefix);

    @Query("""
        SELECT d FROM Doctor d
        WHERE d.status = com.mediflow.platform.doctor.enums.DoctorStatus.ACTIVE
          AND (LOWER(CONCAT(d.firstName, ' ', d.lastName)) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(d.doctorCode) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Doctor> searchActiveDoctors(@Param("q") String q, Pageable pageable);
}
