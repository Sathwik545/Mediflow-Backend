package com.mediflow.platform.auth.enums;

/**
 * Canonical role names used across the authorization system.
 *
 * Roles are stored without the "ROLE_" prefix in the DB.
 * Spring Security receives them prefixed: "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT".
 *
 * ADMIN   — full system access: create doctors/patients, manage all appointments.
 * DOCTOR  — access own appointments, view assigned patient profiles.
 * PATIENT — access own profile and own appointment history.
 */
public enum RoleName {
    ADMIN,
    DOCTOR,
    PATIENT
}
