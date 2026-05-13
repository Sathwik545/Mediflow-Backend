package com.mediflow.platform.auth.jwt;

import com.mediflow.platform.auth.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Central JWT utility — handles token generation, validation, and claims extraction.
 *
 * Uses JJWT 0.12.x API:
 *   - Jwts.builder()        for creating tokens
 *   - Jwts.parser()         for parsing and verifying tokens
 *   - Keys.hmacShaKeyFor()  to derive a SecretKey from the Base64 application secret
 *
 * Token design:
 *   Access tokens  — short-lived (1 h), contain userId + username + roles as claims.
 *   Refresh tokens — NOT JWT; they are opaque UUIDs stored in the DB (see RefreshToken entity).
 *
 * Claim keys are intentionally short to keep token size small.
 */
@Component
@Slf4j
public class JwtUtil {

    // Claim key constants — keep in sync with extraction methods below
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLES   = "roles";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    /**
     * Constructor-based injection reads from application.yaml:
     *   app.jwt.secret                (Base64-encoded HMAC-SHA256 key, min 256 bits)
     *   app.jwt.access-token-expiration  (milliseconds, default 3 600 000 = 1 h)
     */
    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpirationMs
    ) {
        // Decode the Base64 secret into a javax.crypto.SecretKey for HMAC-SHA256
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        log.info("[JWT] Initialized — access token TTL = {} ms ({} minutes)",
                accessTokenExpirationMs, accessTokenExpirationMs / 60_000);
    }

    // ─── Token Generation ──────────────────────────────────────────────────────

    /**
     * Generates a signed JWT access token for the given user identity.
     *
     * Payload claims:
     *   sub   — username (standard JWT subject)
     *   uid   — internal user ID (for ownership validation in services)
     *   roles — list of granted role strings (e.g., ["ROLE_ADMIN"])
     *   iat   — issued-at timestamp
     *   exp   — expiry timestamp (now + accessTokenExpirationMs)
     */
    public String generateAccessToken(Long userId, String username, List<String> roles) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        String token = Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        CLAIM_USER_ID, userId,
                        CLAIM_ROLES,   roles
                ))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();

        log.debug("[JWT] Access token generated for user='{}' userId={} | expires={}", username, userId, expiry);
        return token;
    }

    // ─── Token Validation ──────────────────────────────────────────────────────

    /**
     * Validates a JWT string.
     * Returns true only if the token is well-formed, signature is valid, and it has not expired.
     * All parse/validation errors are caught and logged at DEBUG level; they do NOT propagate —
     * the filter translates the false return into a 401 response.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            log.debug("[JWT] Token validated successfully");
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("[JWT] Token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.debug("[JWT] Unsupported JWT format: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.debug("[JWT] Malformed JWT: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.debug("[JWT] Invalid JWT signature: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.debug("[JWT] Empty or null JWT string: {}", ex.getMessage());
        }
        return false;
    }

    // ─── Claims Extraction ─────────────────────────────────────────────────────

    /** Extracts the subject (username) from a validated JWT. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extracts the internal user ID (uid claim) from a validated JWT. */
    public Long extractUserId(String token) {
        Object uid = parseClaims(token).get(CLAIM_USER_ID);
        // JJWT deserialises numeric claims as Integer for small values, Long for large ones
        if (uid instanceof Integer) return ((Integer) uid).longValue();
        return (Long) uid;
    }

    /** Extracts the roles list claim from a validated JWT. */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) parseClaims(token).get(CLAIM_ROLES);
    }

    /** Returns the expiry Date from a validated JWT — used by the filter for logging. */
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * Checks whether the token is within the last N minutes of its validity window.
     * Used by the filter to issue a proactive refresh header before the token expires.
     *
     * @param token          the JWT access token
     * @param thresholdMs    how many ms before expiry to consider it "close to expiry"
     */
    public boolean isCloseToExpiry(String token, long thresholdMs) {
        Date expiry = extractExpiration(token);
        return (expiry.getTime() - System.currentTimeMillis()) < thresholdMs;
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    /**
     * Parses and verifies the JWT, returning the Claims payload.
     * Throws JJWT exceptions on any failure — callers must handle them.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Exposes access token TTL in seconds — used in AuthResponseDTO.expiresIn. */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1_000;
    }

    /**
     * Validates a token specifically for the refresh flow, surfacing a structured
     * InvalidTokenException on failure (instead of the raw JJWT exceptions).
     */
    public void validateTokenOrThrow(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException ex) {
            log.warn("[JWT] Expired token presented: subject={}", ex.getClaims().getSubject());
            throw new InvalidTokenException("Session expired. Please login again.", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("[JWT] Invalid token presented: {}", ex.getMessage());
            throw new InvalidTokenException("Invalid or malformed token. Please login again.", ex);
        }
    }
}
