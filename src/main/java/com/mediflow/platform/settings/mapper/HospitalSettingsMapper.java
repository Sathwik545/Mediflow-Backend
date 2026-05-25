package com.mediflow.platform.settings.mapper;

import com.mediflow.platform.settings.dto.HospitalSettingsRequestDTO;
import com.mediflow.platform.settings.dto.HospitalSettingsResponseDTO;
import com.mediflow.platform.settings.entity.HospitalSettings;

/**
 * Static mapper between HospitalSettings entity and its DTOs.
 * Follows the same no-MapStruct pattern used across the MediFlow platform.
 */
public class HospitalSettingsMapper {

    private HospitalSettingsMapper() {}

    public static HospitalSettingsResponseDTO toResponseDTO(HospitalSettings settings) {
        return HospitalSettingsResponseDTO.builder()
                .id(settings.getId())
                .hospitalCode(settings.getHospitalCode())
                .hospitalName(settings.getHospitalName())
                .phoneNumber(settings.getPhoneNumber())
                .alternatePhoneNumber(settings.getAlternatePhoneNumber())
                .email(settings.getEmail())
                .supportEmail(settings.getSupportEmail())
                .website(settings.getWebsite())
                .supportPhone(settings.getSupportPhone())
                .addressLine1(settings.getAddressLine1())
                .addressLine2(settings.getAddressLine2())
                .city(settings.getCity())
                .state(settings.getState())
                .postalCode(settings.getPostalCode())
                .country(settings.getCountry())
                .logoUrl(settings.getLogoUrl())
                .gstNumber(settings.getGstNumber())
                .currencyCode(settings.getCurrencyCode())
                .timezone(settings.getTimezone())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .createdBy(settings.getCreatedBy())
                .updatedBy(settings.getUpdatedBy())
                .build();
    }

    /**
     * Copies all DTO fields onto an existing entity in place.
     * Handles null / blank-to-null normalisation for optional fields:
     * blank strings from the request are stored as null so the DB never
     * holds empty strings instead of NULL.
     */
    public static void updateEntityFromDTO(HospitalSettings entity, HospitalSettingsRequestDTO dto) {
        // Required fields — already validated as @NotBlank before reaching here
        entity.setHospitalName(dto.getHospitalName().trim());
        entity.setHospitalCode(dto.getHospitalCode().trim());
        entity.setCurrencyCode(dto.getCurrencyCode().trim());
        entity.setTimezone(dto.getTimezone().trim());

        // Optional fields — normalize blank/empty to null
        entity.setPhoneNumber(blankToNull(dto.getPhoneNumber()));
        entity.setAlternatePhoneNumber(blankToNull(dto.getAlternatePhoneNumber()));
        entity.setEmail(blankToNull(dto.getEmail()));
        entity.setSupportEmail(blankToNull(dto.getSupportEmail()));
        entity.setWebsite(blankToNull(dto.getWebsite()));
        entity.setSupportPhone(blankToNull(dto.getSupportPhone()));
        entity.setAddressLine1(blankToNull(dto.getAddressLine1()));
        entity.setAddressLine2(blankToNull(dto.getAddressLine2()));
        entity.setCity(blankToNull(dto.getCity()));
        entity.setState(blankToNull(dto.getState()));
        entity.setPostalCode(blankToNull(dto.getPostalCode()));
        entity.setCountry(blankToNull(dto.getCountry()));
        entity.setLogoUrl(blankToNull(dto.getLogoUrl()));
        entity.setGstNumber(blankToNull(dto.getGstNumber()));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
