package com.mediflow.platform.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Shared audit superclass for all business entities.
 *
 * Automatically populated by Spring Data JPA auditing:
 * - createdAt / createdBy: set once on INSERT, never updated.
 * - updatedAt / updatedBy: refreshed on every UPDATE.
 *
 * The 'createdBy' / 'updatedBy' values are resolved by SecurityAuditorAware,
 * which extracts the authenticated user's email from the Spring SecurityContext.
 * Audit fields are NEVER accepted from client request payloads — this class is
 * the single source of truth and is entirely infrastructure-controlled.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    // ─── Timestamp fields ─────────────────────────────────────────────────────

    /**
     * UTC timestamp of the INSERT. Never changes after creation.
     * Hibernate DDL: NOT NULL, no default (Spring sets it before first persist).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * UTC timestamp of the last UPDATE. Refreshed on every save.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── Identity fields ──────────────────────────────────────────────────────

    /**
     * Email of the authenticated user who created this record.
     * Resolved by SecurityAuditorAware from the JWT SecurityContext.
     * Falls back to "system" for bootstrap operations (e.g. DataInitializer).
     * nullable=true so pre-audit rows don't fail schema updates.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 255)
    private String createdBy;

    /**
     * Email of the authenticated user who last modified this record.
     * Updated automatically on every merge/save via AuditingEntityListener.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}
