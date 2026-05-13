package com.mediflow.platform.patient.service;

import com.mediflow.platform.patient.dto.PatientCreationResponseDTO;
import com.mediflow.platform.patient.dto.PatientRequestDTO;
import com.mediflow.platform.patient.dto.PatientResponseDTO;
import com.mediflow.platform.patient.enums.PatientStatus;
import org.springframework.data.domain.Page;

public interface PatientService {

    /**
     * Registers a new patient. Creates a linked User account (PATIENT role) with a temp password.
     * Returns both the patient profile and the one-time temporary password for the admin to share.
     */
    PatientCreationResponseDTO createPatient(PatientRequestDTO request);

    PatientResponseDTO getPatientByCode(String patientCode);

    Page<PatientResponseDTO> getAllPatients(int page, int size, PatientStatus status);

    PatientResponseDTO updatePatient(String patientCode, PatientRequestDTO request);

    PatientResponseDTO deactivatePatient(String patientCode);

    Page<PatientResponseDTO> searchPatients(String query, int page, int size);
}
