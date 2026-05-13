package com.mediflow.platform.auth.service.impl;

import com.mediflow.platform.auth.dto.AuthResponseDTO;
import com.mediflow.platform.auth.dto.LoginRequestDTO;
import com.mediflow.platform.auth.dto.RefreshTokenRequestDTO;
import com.mediflow.platform.auth.entity.RefreshToken;
import com.mediflow.platform.auth.entity.User;
import com.mediflow.platform.auth.exception.AccountInactiveException;
import com.mediflow.platform.auth.exception.AccountLockedException;
import com.mediflow.platform.auth.exception.InvalidCredentialsException;
import com.mediflow.platform.auth.exception.InvalidTokenException;
import com.mediflow.platform.auth.enums.UserStatus;
import com.mediflow.platform.auth.jwt.JwtUtil;
import com.mediflow.platform.auth.repository.RefreshTokenRepository;
import com.mediflow.platform.auth.repository.UserRepository;
import com.mediflow.platform.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implements the full authentication workflow for MediFlow's centralized auth system.
 *
 * Login flow:
 *  1. Resolve user by username OR email (either is accepted as login identifier).
 *  2. Validate BCrypt password match.
 *  3. Enforce account status (ACTIVE only).
 *  4. Update last_login_at timestamp.
 *  5. Generate JWT access token (1 hour) and opaque refresh token (7 days).
 *  6. Persist refresh token to DB for revocation support.
 *  7. Return AuthResponseDTO with both tokens.
 *
 * Refresh flow:
 *  1. Look up refresh token in DB.
 *  2. Validate it is not revoked and not expired.
 *  3. Rotate token: revoke old, issue new refresh token.
 *  4. Issue new access token.
 *
 * Logout flow:
 *  1. Revoke all active refresh tokens for the user.
 *  2. (Access token expires naturally — add Redis blacklist for instant invalidation if needed.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil               jwtUtil;
    private final PasswordEncoder       passwordEncoder;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    // ─── Login ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        String identifier = request.getUsernameOrEmail().trim().toLowerCase();
        log.info("[Auth] Login attempt for identifier='{}'", identifier);

        // Step 1: Resolve user by username OR email — whichever the client supplied
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);

        // Step 2: Validate credentials — generic error prevents username enumeration
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("[Auth] Failed login — invalid credentials for identifier='{}'", identifier);
            throw new InvalidCredentialsException();
        }

        // Step 3: Enforce account status before issuing tokens
        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("[Auth] Login blocked — account LOCKED for username='{}'", user.getUsername());
            throw new AccountLockedException(user.getUsername());
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("[Auth] Login blocked — account INACTIVE for username='{}'", user.getUsername());
            throw new AccountInactiveException(user.getUsername());
        }

        // Step 4: Record last login timestamp for audit trail
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Step 5 & 6: Generate tokens and persist refresh token
        List<String> roles = buildRoleList(user);
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = createAndPersistRefreshToken(user.getId());

        log.info("[Auth] Login successful — username='{}' roles={}", user.getUsername(), roles);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .roles(roles)
                .username(user.getUsername())
                .build();
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        log.debug("[Auth] Refresh token request received");

        // Step 1: Find the refresh token record in DB
        RefreshToken storedToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    log.warn("[Auth] Refresh token not found in DB");
                    return new InvalidTokenException("Invalid refresh token. Please login again.");
                });

        // Step 2a: Check if it has been revoked (e.g., by a previous logout or rotation)
        if (storedToken.isRevoked()) {
            log.warn("[Auth] Revoked refresh token presented for userId={}", storedToken.getUserId());
            throw new InvalidTokenException("Refresh token has been revoked. Please login again.");
        }

        // Step 2b: Check absolute expiry
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[Auth] Expired refresh token presented for userId={}", storedToken.getUserId());
            throw new InvalidTokenException("Session expired. Please login again.");
        }

        // Step 3: Load the user to get current roles and status
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User account no longer exists. Please login again."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("[Auth] Refresh blocked — user '{}' is not ACTIVE (status={})",
                    user.getUsername(), user.getStatus());
            throw new InvalidTokenException("Your account is not active. Please contact support.");
        }

        // Step 4: Token rotation — revoke old refresh token, issue a new one
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        List<String> roles = buildRoleList(user);
        String newAccessToken  = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);
        String newRefreshToken = createAndPersistRefreshToken(user.getId());

        log.info("[Auth] Token refreshed — username='{}' newRefreshToken issued", user.getUsername());

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .roles(roles)
                .username(user.getUsername())
                .build();
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String username) {
        log.info("[Auth] Logout request for username='{}'", username);

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // Silently succeed — idempotent logout
            log.debug("[Auth] Logout: user '{}' not found, treating as already logged out", username);
            return;
        }

        // Revoke all active refresh tokens — access tokens expire naturally (JWT stateless)
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("[Auth] All refresh tokens revoked for username='{}'", username);
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /** Builds "ROLE_<NAME>" strings from the user's role set — used in JWT claims and DTO. */
    private List<String> buildRoleList(User user) {
        return user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName().name())
                .toList();
    }

    /**
     * Creates a new refresh token for a user:
     *  1. Cleans up any previously expired or revoked tokens for the same user.
     *  2. Generates a cryptographically random UUID-based token string.
     *  3. Persists it with the configured expiry timestamp.
     */
    private String createAndPersistRefreshToken(Long userId) {
        // Clean up stale tokens before adding a new one — keeps the table lean
        refreshTokenRepository.deleteExpiredOrRevokedByUserId(userId);

        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpirationMs / 1_000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .userId(userId)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("[Auth] Refresh token persisted for userId={} | expires={}", userId, expiresAt);

        return tokenValue;
    }

    /**
     * Derives a unique username from an email address (part before @).
     * Falls back to appending a 4-digit suffix if the base is already taken.
     * Used by DoctorServiceImpl and PatientServiceImpl when creating user accounts.
     */
    public String deriveUniqueUsername(String email) {
        // Strip everything after @ and clean non-standard characters
        String base = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9._]", "");
        if (base.isBlank()) base = "user";

        if (!userRepository.existsByUsername(base)) {
            return base;
        }

        // Try appending a counter (1–99) before falling back to UUID suffix
        for (int i = 1; i <= 99; i++) {
            String candidate = base + i;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        // Final fallback — guaranteed unique
        return base + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    /**
     * Generates a random 12-character temporary password containing uppercase, lowercase,
     * digits and special characters — meets most minimum password requirements.
     */
    public String generateTemporaryPassword() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String special = "!@#$%";
        String all     = upper + lower + digits + special;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(12);

        // Guarantee at least one character from each category
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));

        // Fill remaining 8 positions from the full alphabet
        for (int i = 4; i < 12; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        // Shuffle to avoid predictable positions for guaranteed-category chars
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }
        return new String(chars);
    }
}
