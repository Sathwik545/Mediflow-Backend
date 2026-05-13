package com.mediflow.platform.auth.exception;

/** Thrown when the supplied username/email or password does not match any active account. */
public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid username or password. Please check your credentials and try again.");
    }
}
