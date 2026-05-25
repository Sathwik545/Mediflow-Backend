package com.mediflow.platform.settings.service.impl;

import com.mediflow.platform.common.exception.BusinessRuleViolationException;
import com.mediflow.platform.settings.dto.HospitalSettingsRequestDTO;
import com.mediflow.platform.settings.dto.HospitalSettingsResponseDTO;
import com.mediflow.platform.settings.entity.HospitalSettings;
import com.mediflow.platform.settings.mapper.HospitalSettingsMapper;
import com.mediflow.platform.settings.repository.HospitalSettingsRepository;
import com.mediflow.platform.settings.service.HospitalSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Service implementation for hospital / organization configuration.
 *
 * Single-row invariant: the table must always contain exactly one row.
 * Enforcement strategy:
 *  1. DataInitializer seeds the row at startup (before any request can arrive).
 *  2. updateSettings() calls findTopByOrderByIdAsc() and either updates the
 *     found row OR creates a new one — never creates a second row.
 *  3. No "create" endpoint is exposed. DELETE endpoint is intentionally absent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalSettingsServiceImpl implements HospitalSettingsService {

    private final HospitalSettingsRepository hospitalSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public HospitalSettingsResponseDTO getSettings() {
        // DataInitializer guarantees a row exists; fallback to in-memory default is a safety net only.
        HospitalSettings settings = hospitalSettingsRepository.findTopByOrderByIdAsc()
                .orElseGet(this::buildInMemoryDefault);
        return HospitalSettingsMapper.toResponseDTO(settings);
    }

    @Override
    @Transactional
    public HospitalSettingsResponseDTO updateSettings(HospitalSettingsRequestDTO request) {
        validateTimezone(request.getTimezone());

        // Fetch the existing row or create a new entity (DataInitializer should have seeded it).
        HospitalSettings settings = hospitalSettingsRepository.findTopByOrderByIdAsc()
                .orElse(HospitalSettings.builder().build());

        HospitalSettingsMapper.updateEntityFromDTO(settings, request);

        HospitalSettings saved = hospitalSettingsRepository.save(settings);
        log.info("[HospitalSettings] Settings updated — hospitalCode={}, updatedBy={}",
                saved.getHospitalCode(), saved.getUpdatedBy());
        return HospitalSettingsMapper.toResponseDTO(saved);
    }

    /**
     * Returns an unsaved in-memory default. Used only as a last-resort fallback
     * inside the readOnly getSettings() transaction — no DB write occurs.
     */
    private HospitalSettings buildInMemoryDefault() {
        log.warn("[HospitalSettings] No settings row found — returning in-memory default. " +
                 "DataInitializer should have seeded this on startup.");
        return HospitalSettings.builder()
                .hospitalCode("MEDIFLOW")
                .hospitalName("MediFlow Hospital")
                .currencyCode("INR")
                .timezone("Asia/Kolkata")
                .build();
    }

    /**
     * Validates the timezone string against the JVM's IANA timezone database.
     * Throws BusinessRuleViolationException (422) for invalid zone IDs.
     */
    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new BusinessRuleViolationException(
                "Invalid timezone: '" + timezone + "'. " +
                "Use a valid IANA timezone ID such as 'Asia/Kolkata' or 'America/New_York'."
            );
        }
    }
}
