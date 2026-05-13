package com.mediflow.platform.auth.service;

import com.mediflow.platform.auth.dto.AuthResponseDTO;
import com.mediflow.platform.auth.dto.LoginRequestDTO;
import com.mediflow.platform.auth.dto.RefreshTokenRequestDTO;

/**
 * Contract for centralized authentication operations.
 *
 * All three operations (login, refresh, logout) are stateless at the JWT level;
 * session state lives only in the refresh_tokens table.
 */
public interface AuthService {

    /**
     * Authenticates a user by username/email + password.
     * On success: updates last_login_at, issues access + refresh tokens.
     * On failure: throws InvalidCredentialsException, AccountLockedException, or AccountInactiveException.
     */
    AuthResponseDTO login(LoginRequestDTO request);

    /**
     * Exchanges a valid, non-revoked refresh token for a new access token.
     * Implements token rotation: the old refresh token is revoked and a new one is issued.
     * If the refresh token is expired or revoked: throws InvalidTokenException.
     */
    AuthResponseDTO refreshToken(RefreshTokenRequestDTO request);

    /**
     * Revokes all active refresh tokens for the caller's user account.
     * The access token remains technically valid until it expires (JWT is stateless),
     * but the client should discard it immediately.
     */
    void logout(String username);
}
