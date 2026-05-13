package com.mediflow.platform.auth.exception;

/**
 * Thrown when a JWT or refresh token is malformed, has an invalid signature,
 * is expired, or has been revoked.
 *
 * The message is intentionally generic to avoid leaking token internals to clients.
 * Full details are logged at DEBUG level in JwtUtil and JwtAuthenticationFilter.
 */
public class InvalidTokenException extends AuthException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
