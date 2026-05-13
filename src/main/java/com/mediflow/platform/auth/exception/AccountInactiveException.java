package com.mediflow.platform.auth.exception;

/** Thrown when a login attempt is made against a soft-deleted (INACTIVE) user account. */
public class AccountInactiveException extends AuthException {

    public AccountInactiveException(String username) {
        super("Account '" + username + "' has been deactivated. Please contact the system administrator.");
    }
}
