package com.mediflow.platform.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity for the 'refresh_tokens' table.
 *
 * Refresh tokens implement sliding session expiration:
 * - An access token is short-lived (1 hour).
 * - A refresh token is long-lived (7 days).
 * - The client calls POST /api/v1/auth/refresh to exchange a valid refresh token
 *   for a new access token, effectively extending the session.
 *
 * Token revocation:
 * - Setting `revoked = true` invalidates the token without deleting the record.
 * - Logout revokes the current refresh token.
 * - A new refresh token is issued on every successful /refresh call
 *   (token rotation) to prevent replay attacks.
 *
 * userId stores the owner's database PK directly (no JPA FK relation)
 * to avoid lazy-loading complexity inside stateless filters.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_value",   columnList = "token"),
        @Index(name = "idx_refresh_token_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cryptographically random UUID-based token string. */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /** FK to users.id — stored as a plain Long to avoid session-scope JPA proxy issues. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Absolute expiry timestamp. Token is invalid after this time regardless of revocation flag. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Soft-revocation flag. Set to true on logout or token rotation.
     * A revoked token is rejected immediately even if not yet expired.
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
