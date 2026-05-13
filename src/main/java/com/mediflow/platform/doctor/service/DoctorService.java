package com.mediflow.platform.doctor.service;

import com.mediflow.platform.doctor.dto.DoctorCreationResponseDTO;
import com.mediflow.platform.doctor.dto.DoctorRequestDTO;
import com.mediflow.platform.doctor.dto.DoctorResponseDTO;
import com.mediflow.platform.doctor.enums.DoctorStatus;
import org.springframework.data.domain.Page;

/**
 * Service contract for doctor management operations.
 * All implementations must enforce business rules (uniqueness checks, soft delete, etc.).
 */
public interface DoctorService {

    /**
     * Registers a new doctor. Creates a linked User account (DOCTOR role) with a temp password.
     * Returns both the doctor profile and the one-time temporary password for the admin to share.
     */
    DoctorCreationResponseDTO createDoctor(DoctorRequestDTO request);

    /** Retrieves full doctor details by doctorCode. Throws DoctorNotFoundException if not found. */
    DoctorResponseDTO getDoctorByCode(String doctorCode);

    /**
     * Returns a paginated list of doctors.
     * If status is provided, results are filtered; otherwise all statuses are returned.
     */
    Page<DoctorResponseDTO> getAllDoctors(int page, int size, DoctorStatus status);

    /**
     * Fully replaces a doctor's mutable fields. Re-validates uniqueness constraints
     * excluding the current doctor record. Throws DoctorNotFoundException if not found.
     */
    DoctorResponseDTO updateDoctor(String doctorCode, DoctorRequestDTO request);

    /**
     * Soft-deletes a doctor by setting status to INACTIVE.
     * Returns the deactivated record so callers can confirm the final state.
     */
    DoctorResponseDTO deactivateDoctor(String doctorCode);

    Page<DoctorResponseDTO> searchDoctors(String query, int page, int size);
}
