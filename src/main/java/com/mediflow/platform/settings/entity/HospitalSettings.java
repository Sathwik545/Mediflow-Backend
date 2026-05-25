package com.mediflow.platform.settings.entity;

import com.mediflow.platform.common.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Single-row configuration table for the hospital / organization.
 *
 * Design contract: exactly ONE row exists at all times.
 * The service layer enforces this — there is no "create new hospital" endpoint.
 *
 * @DynamicUpdate ensures Hibernate only includes changed columns in UPDATE
 * statements, important for a wide settings table where only a few fields
 * change on each save.
 *
 * Used as the authoritative source of truth for:
 *  - Invoice / receipt PDF generation (hospitalName, address, GST, currency)
 *  - Notification templates (email, supportEmail, supportPhone)
 *  - Branding (logoUrl)
 *  - Reporting (timezone, currencyCode)
 *
 * Audit fields (createdAt, updatedAt, createdBy, updatedBy) are inherited
 * from BaseAuditEntity and populated automatically via Spring Data JPA auditing.
 */
@Entity
@Table(
    name = "hospital_settings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_hospital_code", columnNames = "hospital_code")
    },
    indexes = {
        @Index(name = "idx_hospital_code", columnList = "hospital_code")
    }
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalSettings extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity ──────────────────────────────────────────────────────────────

    @Column(name = "hospital_code", nullable = false, unique = true, length = 50)
    private String hospitalCode;

    @Column(name = "hospital_name", nullable = false, length = 255)
    private String hospitalName;

    // ── Contact ───────────────────────────────────────────────────────────────

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "alternate_phone_number", length = 20)
    private String alternatePhoneNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "support_email", length = 255)
    private String supportEmail;

    @Column(name = "website", length = 255)
    private String website;

    // ── Address ───────────────────────────────────────────────────────────────

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 100)
    private String country;

    // ── Branding ──────────────────────────────────────────────────────────────

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    // ── Financial / Legal ─────────────────────────────────────────────────────

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "INR";

    @Column(name = "timezone", nullable = false, length = 60)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    // ── Operational ───────────────────────────────────────────────────────────

    @Column(name = "support_phone", length = 20)
    private String supportPhone;

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
