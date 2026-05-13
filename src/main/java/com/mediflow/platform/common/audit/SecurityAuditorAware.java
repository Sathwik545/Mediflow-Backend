package com.mediflow.platform.common.audit;

import com.mediflow.platform.auth.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the current auditor identity for Spring Data JPA auditing.
 *
 * How it works:
 * 1. Reads the Authentication from the Spring SecurityContextHolder.
 * 2. If the principal is a UserPrincipal (JWT-authenticated user), returns their email.
 * 3. Falls back to "system" for:
 *    - Unauthenticated requests (public endpoints)
 *    - Bootstrap operations (DataInitializer creating the admin user at startup)
 *    - Scheduled/background tasks with no user context
 *
 * Security guarantee: audit identity is ALWAYS extracted from the server-side
 * SecurityContext — it is NEVER read from client request payloads.
 *
 * Bean name "securityAuditorAware" must match the auditorAwareRef in @EnableJpaAuditing.
 */
@Component("securityAuditorAware")
@Slf4j
public class SecurityAuditorAware implements AuditorAware<String> {

    /** Fallback identity used when no authenticated user is present in the SecurityContext. */
    private static final String SYSTEM_AUDITOR = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // No authentication in context — bootstrap or unauthenticated public endpoint
        if (authentication == null || !authentication.isAuthenticated()) {
            log.trace("[Auditor] No active authentication — using fallback '{}'", SYSTEM_AUDITOR);
            return Optional.of(SYSTEM_AUDITOR);
        }

        Object principal = authentication.getPrincipal();

        // Anonymous authentication has a String principal ("anonymousUser")
        if (principal instanceof String) {
            log.trace("[Auditor] Anonymous principal — using fallback '{}'", SYSTEM_AUDITOR);
            return Optional.of(SYSTEM_AUDITOR);
        }

        // JWT-authenticated request — extract email directly from UserPrincipal (no DB lookup)
        if (principal instanceof UserPrincipal userPrincipal) {
            String email = userPrincipal.getEmail();
            log.trace("[Auditor] Resolved auditor email: {}", email);
            return Optional.of(email);
        }

        // Unexpected principal type — fall back gracefully rather than throwing
        log.warn("[Auditor] Unknown principal type '{}' — using fallback '{}'",
                principal.getClass().getName(), SYSTEM_AUDITOR);
        return Optional.of(SYSTEM_AUDITOR);
    }
}
