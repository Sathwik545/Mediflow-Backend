package com.mediflow.platform.auth.exception;

/** Thrown when a login attempt is made against a LOCKED user account. */
public class AccountLockedException extends AuthException {

    public AccountLockedException(String username) {
        super("Account '" + username + "' is locked. Please contact the system administrator.");
    }
}
