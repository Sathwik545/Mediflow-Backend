package com.mediflow.platform.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by POST /api/v1/patients (admin creates a new patient).
 *
 * Wraps the standard PatientResponseDTO together with the one-time temporary password.
 * The temporary password is only returned here — it is NEVER stored in plain text
 * and cannot be retrieved again. The admin must communicate it to the patient securely.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreationResponseDTO {

    /** Full patient profile — all non-sensitive business fields. */
    private PatientResponseDTO patient;

    /**
     * Auto-generated temporary password in plain text (only shown once at creation).
     * BCrypt hash of this value is stored in the users table.
     */
    private String temporaryPassword;

    /** Login username auto-derived from the patient's email address. */
    private String username;
}
