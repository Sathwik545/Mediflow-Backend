package com.mediflow.platform.consultation.service;

import com.mediflow.platform.consultation.dto.ConsultationResponseDTO;
import com.mediflow.platform.consultation.dto.ConsultationSaveDTO;
import com.mediflow.platform.consultation.enums.ConsultationStatus;
import org.springframework.data.domain.Page;

public interface ConsultationService {

    /**
     * Creates a DRAFT consultation from an IN_PROGRESS appointment.
     * Only the doctor assigned to the appointment (or ADMIN) may call this.
     * Rejects if appointment is not IN_PROGRESS or a consultation already exists.
     */
    ConsultationResponseDTO startConsultation(String appointmentCode);

    /**
     * Saves clinical data to a DRAFT consultation without completing it.
     * Allows partial saves — no clinical fields are required.
     * Locks rejected if consultation is already COMPLETED.
     */
    ConsultationResponseDTO saveDraft(String consultationCode, ConsultationSaveDTO request);

    /**
     * Completes a consultation, validating required clinical fields.
     * Automatically transitions the linked appointment to COMPLETED.
     * Locked after completion — no further edits allowed.
     */
    ConsultationResponseDTO completeConsultation(String consultationCode, ConsultationSaveDTO request);

    ConsultationResponseDTO getConsultationByCode(String consultationCode);

    /** Admin-only: paginated list of all consultations, optionally filtered by status. */
    Page<ConsultationResponseDTO> getAllConsultations(ConsultationStatus status, int page, int size);

    Page<ConsultationResponseDTO> getConsultationsByPatient(
            String patientCode, ConsultationStatus status, int page, int size);

    Page<ConsultationResponseDTO> getConsultationsByDoctor(
            String doctorCode, ConsultationStatus status, int page, int size);
}
