package com.mediflow.platform.auth.entity;

import com.mediflow.platform.auth.enums.UserStatus;
import com.mediflow.platform.common.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for the 'users' table — the central identity record for every person
 * in the system, regardless of whether they are an admin, doctor, or patient.
 *
 * Design principles:
 * - Passwords are ALWAYS stored as BCrypt hashes. Plain text is NEVER stored.
 * - username, email, and phone_number are unique identifiers (any can be used at login).
 * - status controls access: only ACTIVE users may authenticate.
 * - Roles are loaded EAGERLY because every JWT validation must check granted authorities,
 *   and lazy-loading inside the stateless filter creates N+1 session problems.
 * - The internal 'id' must never be exposed in API responses; use username or business codes.
 * - Audit fields (createdAt, updatedAt, createdBy, updatedBy) are inherited from
 *   BaseAuditEntity and populated automatically via Spring Data JPA auditing +
 *   SecurityAuditorAware (email from JWT SecurityContext).
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_username",     columnNames = "username"),
        @UniqueConstraint(name = "uq_user_email",        columnNames = "email"),
        @UniqueConstraint(name = "uq_user_phone_number", columnNames = "phone_number")
    },
    indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email",    columnList = "email"),
        @Index(name = "idx_user_status",   columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login handle — lowercase alphanumeric, auto-derived from email at creation. */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    /**
     * BCrypt-hashed password. NEVER expose this field in API responses.
     * column length = 60 (BCrypt always produces exactly 60 chars).
     */
    @Column(name = "password", nullable = false, length = 60)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /** Updated on every successful login for audit and analytics. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Roles are EAGER-loaded so UserDetails is fully populated without an open session.
     * The join table 'user_roles' has no extra columns — it is a pure association.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id",  referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "role_id",  referencedColumnName = "id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
