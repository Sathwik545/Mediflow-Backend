package com.mediflow.platform.common.exception;

import com.mediflow.platform.auth.exception.AccountInactiveException;
import com.mediflow.platform.auth.exception.AccountLockedException;
import com.mediflow.platform.auth.exception.AuthException;
import com.mediflow.platform.auth.exception.InvalidTokenException;
import com.mediflow.platform.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── Auth / Security Exceptions ───────────────────────────────────────────

    /**
     * Catches our custom AuthException hierarchy (InvalidCredentials, AccountLocked, etc.).
     * All auth failures return 401 with the exception's message — intentionally generic
     * for InvalidCredentials to prevent user enumeration.
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
        log.warn("[Security] Authentication failure: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catches expired / revoked / malformed token failures surfaced as InvalidTokenException.
     * Clients must display "Session expired. Please login again." and redirect to login.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(InvalidTokenException ex) {
        log.warn("[Security] Invalid token: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catches AccountLockedException — the account exists but is administratively locked.
     * Returned as 401 (not 423) to avoid leaking account state to external callers.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(AccountLockedException ex) {
        log.warn("[Security] Account locked: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catches AccountInactiveException — the account has been deactivated.
     * Returned as 401 to avoid leaking account state.
     */
    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountInactive(AccountInactiveException ex) {
        log.warn("[Security] Account inactive: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catches Spring Security's BadCredentialsException (from DaoAuthenticationProvider).
     * Mapped to a generic 401 message — no detail to prevent credential enumeration.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[Security] Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password. Please check your credentials and try again."));
    }

    /**
     * Catches Spring Security's DisabledException (user is disabled/inactive).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        log.warn("[Security] Disabled account: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Your account has been deactivated. Please contact support."));
    }

    /**
     * Catches Spring Security's LockedException (account locked by provider).
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        log.warn("[Security] Locked account: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Your account is locked. Please contact the administrator."));
    }

    /**
     * Catches Spring Security's AccessDeniedException (authenticated but lacks required role).
     * Note: for filter/handler-level access denial, AuthAccessDeniedHandler writes directly;
     * this handler covers @PreAuthorize rejections that propagate through the MVC layer.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[Security] Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied. You do not have the required permissions to perform this action."));
    }

    // ─── Domain / Resource Exceptions ─────────────────────────────────────────

    /**
     * Handles all "not found" exceptions across every domain module
     * (PatientNotFoundException, DoctorNotFoundException, etc.).
     * Adding a new module never requires touching this file — just extend ResourceNotFoundException.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles all "already exists / conflict" exceptions across every domain module
     * (PatientAlreadyExistsException, DoctorAlreadyExistsException, etc.).
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        log.warn("Duplicate resource conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        log.debug("Request validation failed with {} error(s)", errors.size());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError(
                    "Validation failed. Please correct the highlighted fields and try again.",
                    errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String fieldName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.put(fieldName, violation.getMessage());
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError(
                    "Validation failed. Please correct the highlighted fields and try again.",
                    errors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed or unreadable request body: {}", ex.getMessage());
        String message = ex.getMessage() != null && ex.getMessage().contains("Cannot deserialize value of type")
                ? "One or more fields contain invalid values. Please verify the accepted values and try again."
                : "Invalid request format. Please check your input and try again.";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': value was '{}'", ex.getName(), ex.getValue());
        String message = String.format(
            "Invalid value '%s' provided for parameter '%s'", ex.getValue(), ex.getName()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                    "The request could not be completed due to a data conflict. The record may already exist."
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("The requested resource was not found"));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity
                .status(422)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "An unexpected error occurred. Please try again later or contact support if the issue persists."
                ));
    }
}
