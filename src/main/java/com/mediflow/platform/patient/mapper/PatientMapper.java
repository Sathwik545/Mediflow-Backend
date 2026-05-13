package com.mediflow.platform.patient.mapper;

import com.mediflow.platform.patient.dto.PatientRequestDTO;
import com.mediflow.platform.patient.dto.PatientResponseDTO;
import com.mediflow.platform.patient.entity.Patient;
import com.mediflow.platform.patient.enums.PatientStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatientMapper {

    public static Patient toEntity(PatientRequestDTO dto, String patientCode) {
        return Patient.builder()
                .patientCode(patientCode)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .bloodGroup(dto.getBloodGroup())
                .phoneNumber(dto.getPhoneNumber().trim())
                .email(dto.getEmail().toLowerCase().trim())
                .addressLine1(dto.getAddressLine1().trim())
                .addressLine2(dto.getAddressLine2() != null ? dto.getAddressLine2().trim() : null)
                .city(dto.getCity().trim())
                .state(dto.getState().trim())
                .postalCode(dto.getPostalCode().trim())
                .emergencyContactName(dto.getEmergencyContactName().trim())
                .emergencyContactPhone(dto.getEmergencyContactPhone().trim())
                .allergies(dto.getAllergies())
                .medicalHistory(dto.getMedicalHistory())
                .status(PatientStatus.ACTIVE)
                .build();
    }

    public static void updateEntityFromDTO(Patient patient, PatientRequestDTO dto) {
        patient.setFirstName(dto.getFirstName().trim());
        patient.setLastName(dto.getLastName().trim());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(dto.getGender());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setPhoneNumber(dto.getPhoneNumber().trim());
        patient.setEmail(dto.getEmail().toLowerCase().trim());
        patient.setAddressLine1(dto.getAddressLine1().trim());
        patient.setAddressLine2(dto.getAddressLine2() != null ? dto.getAddressLine2().trim() : null);
        patient.setCity(dto.getCity().trim());
        patient.setState(dto.getState().trim());
        patient.setPostalCode(dto.getPostalCode().trim());
        patient.setEmergencyContactName(dto.getEmergencyContactName().trim());
        patient.setEmergencyContactPhone(dto.getEmergencyContactPhone().trim());
        patient.setAllergies(dto.getAllergies());
        patient.setMedicalHistory(dto.getMedicalHistory());
    }

    public static PatientResponseDTO toResponseDTO(Patient patient) {
        return PatientResponseDTO.builder()
                .patientCode(patient.getPatientCode())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .fullName(patient.getFirstName() + " " + patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .bloodGroup(patient.getBloodGroup())
                .phoneNumber(patient.getPhoneNumber())
                .email(patient.getEmail())
                .addressLine1(patient.getAddressLine1())
                .addressLine2(patient.getAddressLine2())
                .city(patient.getCity())
                .state(patient.getState())
                .postalCode(patient.getPostalCode())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .allergies(patient.getAllergies())
                .medicalHistory(patient.getMedicalHistory())
                .status(patient.getStatus())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .createdBy(patient.getCreatedBy())
                .updatedBy(patient.getUpdatedBy())
                .build();
    }
}
