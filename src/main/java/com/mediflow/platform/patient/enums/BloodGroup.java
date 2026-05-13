package com.mediflow.platform.patient.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BloodGroup {

    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String label;

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static BloodGroup fromValue(String value) {
        if (value == null) return null;
        for (BloodGroup bg : values()) {
            if (bg.label.equalsIgnoreCase(value) || bg.name().equalsIgnoreCase(value)) {
                return bg;
            }
        }
        throw new IllegalArgumentException(
            "Invalid blood group '" + value + "'. Accepted values are: A+, A-, B+, B-, AB+, AB-, O+, O-"
        );
    }
}
