package com.mediflow.platform.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by POST /api/v1/doctors (admin creates a new doctor).
 *
 * Wraps the standard DoctorResponseDTO together with the one-time temporary password.
 * The temporary password is only returned here — it is NEVER stored in plain text
 * and cannot be retrieved again. The admin must communicate it to the doctor securely.
 *
 * The doctor must change this password via the forgot-password or profile-update flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreationResponseDTO {

    /** Full doctor profile — all non-sensitive business fields. */
    private DoctorResponseDTO doctor;

    /**
     * Auto-generated temporary password in plain text (only shown once at creation).
     * BCrypt hash of this value is stored in the users table.
     */
    private String temporaryPassword;

    /** Login username auto-derived from the doctor's email address. */
    private String username;
}
