package com.mediflow.platform.consultation.repository;

import com.mediflow.platform.consultation.entity.Consultation;
import com.mediflow.platform.consultation.enums.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByConsultationCode(String consultationCode);

    Optional<Consultation> findByAppointment_AppointmentCode(String appointmentCode);

    boolean existsByAppointment_AppointmentCode(String appointmentCode);

    Page<Consultation> findByPatient_PatientCode(String patientCode, Pageable pageable);

    Page<Consultation> findByDoctor_DoctorCode(String doctorCode, Pageable pageable);

    Page<Consultation> findByDoctor_DoctorCodeAndConsultationStatus(
            String doctorCode, ConsultationStatus status, Pageable pageable);

    Page<Consultation> findByPatient_PatientCodeAndConsultationStatus(
            String patientCode, ConsultationStatus status, Pageable pageable);

    Page<Consultation> findByConsultationStatus(ConsultationStatus status, Pageable pageable);

    /** Used by the sequential code generator to find the last assigned CONS code for the current year. */
    Optional<Consultation> findTopByConsultationCodeStartingWithOrderByConsultationCodeDesc(String prefix);
}
