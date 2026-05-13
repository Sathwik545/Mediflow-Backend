package com.mediflow.platform.doctor.enums;

/**
 * Represents the operational status of a doctor in the MediFlow system.
 *
 * ACTIVE   - Available for consultations and appointments (default on registration)
 * ON_LEAVE - Temporarily unavailable (vacation, medical leave, etc.) — not soft-deleted
 * INACTIVE - Soft-deleted or permanently no longer practicing at this facility
 *
 * Only INACTIVE is used for the soft-delete operation (DELETE endpoint).
 * ON_LEAVE can be set via the UPDATE endpoint when the doctor is temporarily away.
 */
public enum DoctorStatus {
    ACTIVE,
    ON_LEAVE,
    INACTIVE
}
