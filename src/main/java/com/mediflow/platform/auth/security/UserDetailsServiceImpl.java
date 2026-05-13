package com.mediflow.platform.auth.security;

import com.mediflow.platform.auth.entity.User;
import com.mediflow.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security's bridge to our User repository.
 *
 * This service is called in two contexts:
 *  1. During form/password authentication — DaoAuthenticationProvider calls it
 *     with the submitted username to load the UserDetails for BCrypt comparison.
 *  2. During JWT filter processing — after extracting the username from a valid JWT,
 *     the filter calls this to reload the full UserDetails (including current status).
 *
 * We always load from the DB (not from the JWT claims) so that a locked or deactivated
 * account is caught immediately on the next request, even if their JWT hasn't expired yet.
 *
 * Roles are EAGERLY fetched on the User entity to avoid lazy-loading inside the
 * stateless filter where there is no Hibernate session.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by username. The parameter may actually be an email address —
     * the login endpoint passes whichever identifier the client supplied.
     *
     * For JWT filter calls, it is always the 'username' extracted from the JWT subject.
     *
     * Throws UsernameNotFoundException (not our custom AuthException) because Spring Security
     * catches UsernameNotFoundException internally during DaoAuthenticationProvider execution.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("[UserDetailsService] Loading user by username='{}'", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[UserDetailsService] No user found for username='{}'", username);
                    // Generic message to prevent user enumeration
                    return new UsernameNotFoundException("User not found");
                });

        log.debug("[UserDetailsService] Loaded user='{}' status={} roles={}",
                user.getUsername(), user.getStatus(), user.getRoles().size());

        return new UserPrincipal(user);
    }
}
