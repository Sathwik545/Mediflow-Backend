package com.mediflow.platform.settings.service;

import com.mediflow.platform.settings.dto.HospitalSettingsRequestDTO;
import com.mediflow.platform.settings.dto.HospitalSettingsResponseDTO;

public interface HospitalSettingsService {

    /**
     * Returns the current hospital settings.
     * If the row was never seeded, returns an in-memory default without persisting —
     * the DataInitializer guarantees the row exists before any request can arrive.
     */
    HospitalSettingsResponseDTO getSettings();

    /**
     * Updates the single hospital settings row.
     * Creates the row if it somehow does not yet exist (safety net for edge cases).
     * Validates the timezone as a valid IANA zone ID before persisting.
     */
    HospitalSettingsResponseDTO updateSettings(HospitalSettingsRequestDTO request);
}
