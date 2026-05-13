package com.mediflow.platform.auth.controller;

import com.mediflow.platform.auth.dto.AuthResponseDTO;
import com.mediflow.platform.auth.dto.LoginRequestDTO;
import com.mediflow.platform.auth.dto.RefreshTokenRequestDTO;
import com.mediflow.platform.auth.security.UserPrincipal;
import com.mediflow.platform.auth.service.AuthService;
import com.mediflow.platform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for centralized authentication.
 *
 * All endpoints under /api/v1/auth/** are PUBLIC (configured in SecurityConfig).
 * No Bearer token is required to call login or refresh.
 *
 * Logout DOES require an authenticated request (valid Bearer token) so the server
 * knows whose refresh tokens to revoke.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Centralized login, token refresh, and logout for all user roles")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/login
     *
     * Accepts username or email + password.
     * Returns access token (1 h), refresh token (7 days), roles, and expiresIn.
     */
    @Operation(
        summary = "User login",
        description = "Authenticates any user (ADMIN / DOCTOR / PATIENT) by username or email + password. " +
                      "Returns a JWT access token and a long-lived refresh token."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        log.info("[AuthController] Login request for identifier='{}'", request.getUsernameOrEmail());
        AuthResponseDTO authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * POST /api/v1/auth/refresh
     *
     * Exchanges a valid refresh token for a new access token + new refresh token.
     * Implements token rotation: the submitted refresh token is immediately revoked.
     * If the refresh token is expired or revoked, a 401 is returned.
     */
    @Operation(
        summary = "Refresh access token",
        description = "Exchanges a valid refresh token for a new access token (sliding session). " +
                      "The submitted refresh token is rotated — a new one is issued in the response."
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO request) {

        log.debug("[AuthController] Token refresh request");
        AuthResponseDTO authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    /**
     * POST /api/v1/auth/logout
     *
     * Revokes all active refresh tokens for the authenticated user.
     * Requires a valid Bearer token (enforced by SecurityFilterChain — this endpoint is secured).
     * The access token cannot be invalidated (JWT is stateless) but expires in 1 hour.
     */
    @Operation(
        summary = "Logout",
        description = "Revokes all active refresh tokens for the current user. " +
                      "Requires a valid Bearer token. The access token expires naturally (1 hour)."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[AuthController] Logout request for username='{}'", principal.getUsername());
        authService.logout(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
            "Logged out successfully. Please discard your access token.", null));
    }
}
