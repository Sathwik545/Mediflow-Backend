package com.mediflow.platform.consultation.mapper;

import com.mediflow.platform.consultation.dto.ConsultationResponseDTO;
import com.mediflow.platform.consultation.dto.PrescriptionItemResponseDTO;
import com.mediflow.platform.consultation.entity.Consultation;
import com.mediflow.platform.consultation.entity.PrescriptionItem;
import com.mediflow.platform.patient.entity.Patient;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;

public class ConsultationMapper {

    private ConsultationMapper() {}

    public static ConsultationResponseDTO toResponseDTO(Consultation c) {
        return ConsultationResponseDTO.builder()
                .id(c.getId())
                .consultationCode(c.getConsultationCode())

                // Appointment context
                .appointmentCode(c.getAppointment().getAppointmentCode())
                .consultationType(c.getAppointment().getConsultationType())
                .appointmentDate(c.getAppointment().getAppointmentDate())
                .startTime(c.getAppointment().getStartTime())
                .endTime(c.getAppointment().getEndTime())

                // Patient (from relationship + computed age)
                .patientCode(c.getPatient().getPatientCode())
                .patientName(c.getPatientNameSnapshot())
                .patientGender(c.getPatient().getGender() != null
                        ? c.getPatient().getGender().name() : null)
                .patientAge(computeAge(c.getPatient()))

                // Doctor (from relationship)
                .doctorCode(c.getDoctor().getDoctorCode())
                .doctorName(c.getDoctorNameSnapshot())
                .doctorSpecialization(c.getDoctor().getSpecialization())
                .doctorDepartment(c.getDoctor().getDepartment())

                // Vitals
                .bloodPressure(c.getBloodPressure())
                .pulseRate(c.getPulseRate())
                .temperature(c.getTemperature())
                .oxygenSaturation(c.getOxygenSaturation())
                .respiratoryRate(c.getRespiratoryRate())
                .height(c.getHeight())
                .weight(c.getWeight())
                .bmi(c.getBmi())

                // Clinical
                .chiefComplaint(c.getChiefComplaint())
                .symptoms(c.getSymptoms())
                .diagnosis(c.getDiagnosis())
                .doctorNotes(c.getDoctorNotes())
                .treatmentPlan(c.getTreatmentPlan())

                // Follow-up
                .followUpRequired(c.getFollowUpRequired())
                .followUpDate(c.getFollowUpDate())
                .followUpNotes(c.getFollowUpNotes())

                // Status
                .consultationStatus(c.getConsultationStatus())

                // Prescriptions
                .prescriptionItems(mapPrescriptions(c.getPrescriptionItems()))

                // Audit
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .createdBy(c.getCreatedBy())
                .updatedBy(c.getUpdatedBy())

                .build();
    }

    private static List<PrescriptionItemResponseDTO> mapPrescriptions(
            List<PrescriptionItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> PrescriptionItemResponseDTO.builder()
                        .id(item.getId())
                        .medicineName(item.getMedicineName())
                        .dosage(item.getDosage())
                        .frequency(item.getFrequency())
                        .duration(item.getDuration())
                        .instructions(item.getInstructions())
                        .build())
                .toList();
    }

    private static Integer computeAge(Patient patient) {
        if (patient == null || patient.getDateOfBirth() == null) {
            return null;
        }
        return Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
    }
}
