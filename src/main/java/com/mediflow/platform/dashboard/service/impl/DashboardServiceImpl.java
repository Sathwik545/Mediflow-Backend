package com.mediflow.platform.dashboard.service.impl;

import com.mediflow.platform.appointment.repository.AppointmentRepository;
import com.mediflow.platform.dashboard.dto.DashboardStatsDTO;
import com.mediflow.platform.dashboard.service.DashboardService;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import com.mediflow.platform.doctor.repository.DoctorRepository;
import com.mediflow.platform.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats() {
        long totalPatients     = patientRepository.count();
        long activeDoctors     = doctorRepository.countByStatus(DoctorStatus.ACTIVE);
        long todayAppointments = appointmentRepository.countByAppointmentDate(LocalDate.now());

        return DashboardStatsDTO.builder()
                .totalPatients(totalPatients)
                .activeDoctors(activeDoctors)
                .todayAppointments(todayAppointments)
                .monthlyRevenue(0L)
                .patientSub("All registered patients")
                .doctorSub("Currently active")
                .appointmentSub("Scheduled for today")
                .revenueSub("Billing not yet available")
                .build();
    }
}
