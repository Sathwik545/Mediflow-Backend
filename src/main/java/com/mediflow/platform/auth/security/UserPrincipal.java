package com.mediflow.platform.auth.security;

import com.mediflow.platform.auth.entity.User;
import com.mediflow.platform.auth.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Wrapper that adapts the MediFlow {@link User} entity to Spring Security's {@link UserDetails}.
 *
 * Why a custom wrapper instead of Spring's built-in User builder?
 * The built-in org.springframework.security.core.userdetails.User does not carry
 * the internal userId. Downstream services need the userId from the security context
 * to enforce ownership validation (e.g., "is this patient's userId the logged-in user's id?").
 *
 * Roles are stored in the DB without the "ROLE_" prefix (e.g., "ADMIN").
 * Spring Security requires the "ROLE_" prefix for role-based @PreAuthorize checks,
 * so it is added here during authority mapping.
 */
@Getter
public class UserPrincipal implements UserDetails {

    /** Internal database PK — used for ownership validation in services. */
    private final Long userId;
    private final String username;

    /**
     * User's email — used by SecurityAuditorAware to populate createdBy/updatedBy
     * audit fields without requiring an extra DB lookup on every save.
     */
    private final String email;

    // Password is loaded for BCrypt verification during authentication.
    // It is NEVER serialized into JWT or returned in API responses.
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;
    private final UserStatus status;

    public UserPrincipal(User user) {
        this.userId   = user.getId();
        this.username = user.getUsername();
        this.email    = user.getEmail();
        this.password = user.getPassword();
        this.status   = user.getStatus();

        // Map each RoleName enum to "ROLE_<NAME>" granted authority
        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
    }

    // ─── UserDetails contract ──────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** Account is never considered "expired" — lifecycle is managed via status field. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    /** Credentials (password) never expire — users change passwords via a dedicated flow. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Only ACTIVE users are considered enabled; INACTIVE and LOCKED are rejected. */
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    // Role convenience helpers for use in filter/service logic ─────────────────

    public List<String> getRoleNames() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
