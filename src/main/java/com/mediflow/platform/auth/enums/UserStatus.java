package com.mediflow.platform.auth.enums;

/**
 * Lifecycle states for a User account.
 *
 * ACTIVE  — normal state; can authenticate and access APIs.
 * INACTIVE — soft-deleted; login rejected with 401, not 404, to avoid user enumeration.
 * LOCKED  — locked after suspicious activity or admin action; login rejected with 423-like 401.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED
}
