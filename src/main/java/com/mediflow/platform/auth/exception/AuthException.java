package com.mediflow.platform.auth.exception;

/**
 * Base exception for all authentication and authorization failures.
 * Subclasses carry specific semantics (bad credentials, locked account, expired token, etc.).
 * GlobalExceptionHandler maps this hierarchy to 401 Unauthorized responses.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
