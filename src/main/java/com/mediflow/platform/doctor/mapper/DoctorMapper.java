package com.mediflow.platform.doctor.mapper;

import com.mediflow.platform.doctor.dto.DoctorRequestDTO;
import com.mediflow.platform.doctor.dto.DoctorResponseDTO;
import com.mediflow.platform.doctor.entity.Doctor;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Stateless utility class for mapping between Doctor entity, request DTO, and response DTO.
 *
 * Normalization rules applied during mapping:
 * - Strings are trimmed to remove accidental leading/trailing whitespace.
 * - Email is lowercased to enforce case-insensitive uniqueness.
 * - License number is uppercased to enforce a consistent storage format.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DoctorMapper {

    /**
     * Converts a validated DoctorRequestDTO into a new Doctor entity.
     * The generated doctorCode is injected here; status defaults to ACTIVE.
     */
    public static Doctor toEntity(DoctorRequestDTO dto, String doctorCode) {
        return Doctor.builder()
                .doctorCode(doctorCode)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .email(dto.getEmail().toLowerCase().trim())
                .phoneNumber(dto.getPhoneNumber().trim())
                .specialization(dto.getSpecialization().trim())
                .qualification(dto.getQualification().trim())
                .yearsOfExperience(dto.getYearsOfExperience())
                .consultationFee(dto.getConsultationFee())
                .licenseNumber(dto.getLicenseNumber().trim().toUpperCase())
                .department(dto.getDepartment().trim())
                .status(DoctorStatus.ACTIVE)
                .build();
    }

    /**
     * Applies all mutable fields from the DTO onto an existing Doctor entity (for PUT/update).
     * doctorCode, id, createdAt, and status are intentionally not overwritten here.
     */
    public static void updateEntityFromDTO(Doctor doctor, DoctorRequestDTO dto) {
        doctor.setFirstName(dto.getFirstName().trim());
        doctor.setLastName(dto.getLastName().trim());
        doctor.setEmail(dto.getEmail().toLowerCase().trim());
        doctor.setPhoneNumber(dto.getPhoneNumber().trim());
        doctor.setSpecialization(dto.getSpecialization().trim());
        doctor.setQualification(dto.getQualification().trim());
        doctor.setYearsOfExperience(dto.getYearsOfExperience());
        doctor.setConsultationFee(dto.getConsultationFee());
        doctor.setLicenseNumber(dto.getLicenseNumber().trim().toUpperCase());
        doctor.setDepartment(dto.getDepartment().trim());
        if (dto.getStatus() != null) {
            doctor.setStatus(dto.getStatus());
        }
    }

    /**
     * Converts a Doctor entity to the outbound DoctorResponseDTO.
     * The internal 'id' field is excluded — only 'doctorCode' is surfaced.
     */
    public static DoctorResponseDTO toResponseDTO(Doctor doctor) {
        return DoctorResponseDTO.builder()
                .doctorCode(doctor.getDoctorCode())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .fullName(doctor.getFirstName() + " " + doctor.getLastName())
                .email(doctor.getEmail())
                .phoneNumber(doctor.getPhoneNumber())
                .specialization(doctor.getSpecialization())
                .qualification(doctor.getQualification())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .consultationFee(doctor.getConsultationFee())
                .licenseNumber(doctor.getLicenseNumber())
                .department(doctor.getDepartment())
                .status(doctor.getStatus())
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .createdBy(doctor.getCreatedBy())
                .updatedBy(doctor.getUpdatedBy())
                .build();
    }
}
