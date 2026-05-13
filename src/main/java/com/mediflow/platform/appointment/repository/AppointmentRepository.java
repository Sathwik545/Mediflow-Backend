package com.mediflow.platform.appointment.repository;

import com.mediflow.platform.appointment.entity.Appointment;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;



@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByAppointmentCode(String appointmentCode);

    // Used by code generation — mirrors DoctorRepository.findTopByDoctorCodeStartingWith pattern
    Optional<Appointment> findTopByAppointmentCodeStartingWithOrderByAppointmentCodeDesc(String prefix);

    // Overlap detection: finds conflicting slots for the same doctor on the same date,
    // excluding already cancelled/no-show appointments.
    // Two intervals [s1,e1) and [s2,e2) overlap when s1 < e2 AND e1 > s2.
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.doctor.id = :doctorId
          AND a.appointmentDate = :date
          AND a.appointmentStatus NOT IN (
              com.mediflow.platform.appointment.enums.AppointmentStatus.CANCELLED,
              com.mediflow.platform.appointment.enums.AppointmentStatus.NO_SHOW
          )
          AND a.startTime < :endTime
          AND a.endTime > :startTime
        """)
    List<Appointment> findOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    long countByAppointmentDate(LocalDate date);

    Page<Appointment> findByAppointmentStatus(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByPatient_PatientCode(String patientCode, Pageable pageable);

    Page<Appointment> findByDoctor_DoctorCode(String doctorCode, Pageable pageable);

    // Used by the available-slots endpoint to find booked intervals for a doctor on a given date.
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.doctor.id = :doctorId
          AND a.appointmentDate = :date
          AND a.appointmentStatus NOT IN (
              com.mediflow.platform.appointment.enums.AppointmentStatus.CANCELLED,
              com.mediflow.platform.appointment.enums.AppointmentStatus.NO_SHOW
          )
        """)
    List<Appointment> findActiveByDoctorAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date
    );

    // Used by deactivation guards in PatientServiceImpl and DoctorServiceImpl
    boolean existsByPatient_PatientCodeAndAppointmentDateAfterAndAppointmentStatusIn(
            String patientCode, LocalDate date, List<AppointmentStatus> statuses);

    boolean existsByDoctor_DoctorCodeAndAppointmentDateAfterAndAppointmentStatusIn(
            String doctorCode, LocalDate date, List<AppointmentStatus> statuses);
}
