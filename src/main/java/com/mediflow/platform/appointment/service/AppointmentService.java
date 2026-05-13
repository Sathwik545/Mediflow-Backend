package com.mediflow.platform.appointment.service;

import com.mediflow.platform.appointment.dto.AppointmentRequestDTO;
import com.mediflow.platform.appointment.dto.AppointmentResponseDTO;
import com.mediflow.platform.appointment.dto.TimeSlotDTO;
import com.mediflow.platform.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    AppointmentResponseDTO bookAppointment(AppointmentRequestDTO request);

    AppointmentResponseDTO getAppointmentByCode(String appointmentCode);

    Page<AppointmentResponseDTO> getAllAppointments(int page, int size, AppointmentStatus status);

    Page<AppointmentResponseDTO> getAppointmentsByPatient(String patientCode, int page, int size);

    Page<AppointmentResponseDTO> getAppointmentsByDoctor(String doctorCode, int page, int size);

    AppointmentResponseDTO cancelAppointment(String appointmentCode);

    AppointmentResponseDTO completeAppointment(String appointmentCode);

    /**
     * Returns the 30-minute time slots for a given doctor on a given date,
     * each marked as available or booked. Slots span 09:00–17:30.
     */
    List<TimeSlotDTO> getAvailableSlots(String doctorCode, LocalDate date);
}
