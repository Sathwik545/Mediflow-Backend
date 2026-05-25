package com.mediflow.platform.config;

import com.mediflow.platform.auth.filter.JwtAuthenticationFilter;
import com.mediflow.platform.auth.handler.AuthAccessDeniedHandler;
import com.mediflow.platform.auth.handler.AuthEntryPoint;
import com.mediflow.platform.auth.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration for the MediFlow platform.
 *
 * ── Architecture decisions ────────────────────────────────────────────────────
 *
 * 1. STATELESS sessions: no HTTP sessions are created; all state lives in JWT.
 * 2. CSRF disabled:      not needed for stateless REST APIs (tokens are not cookies).
 * 3. JWT filter:         runs before UsernamePasswordAuthenticationFilter on every request.
 * 4. Role-based paths:   coarse-grained access at URL level; fine-grained via @PreAuthorize.
 * 5. Custom handlers:    AuthEntryPoint (401) and AuthAccessDeniedHandler (403) return JSON,
 *                        replacing Spring's default HTML error pages.
 * 6. BCrypt strength 12: strong enough for production; balances security vs. CPU cost.
 *
 * ── RBAC path summary ─────────────────────────────────────────────────────────
 *
 *  PUBLIC (no token required):
 *    POST /api/v1/auth/login
 *    POST /api/v1/auth/refresh
 *    GET  /swagger-ui/**, /v3/api-docs/**
 *
 *  ADMIN only:
 *    POST /api/v1/doctors      (create doctor — also creates user account)
 *    POST /api/v1/patients     (create patient — also creates user account)
 *    DELETE /api/v1/doctors/** (deactivate doctor)
 *    DELETE /api/v1/patients** (deactivate patient)
 *
 *  ADMIN + DOCTOR:
 *    PUT  /api/v1/appointments/{code}/complete
 *    PUT  /api/v1/appointments/{code}/cancel
 *
 *  ADMIN + DOCTOR + PATIENT (any authenticated user):
 *    GET  /api/v1/**
 *    PUT  /api/v1/doctors/**
 *    PUT  /api/v1/patients/**
 *
 *  AUTHENTICATED (any valid JWT):
 *    POST /api/v1/auth/logout
 *    POST /api/v1/appointments  (booking — admin or patient)
 *    All other /api/v1/** routes
 *
 * Fine-grained ownership validation (e.g. "patient can only see their own record")
 * is enforced in the service layer via JWT userId extraction, NOT here.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize, @PostAuthorize in controllers/services
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml"
    };

    private final JwtAuthenticationFilter  jwtAuthFilter;
    private final AuthEntryPoint           authEntryPoint;
    private final AuthAccessDeniedHandler  accessDeniedHandler;
    private final UserDetailsServiceImpl   userDetailsService;

    // ─── Security Filter Chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] Configuring stateless JWT security filter chain");

        http
            // ── Disable CSRF — REST APIs use tokens, not cookies ──────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Stateless sessions — no HttpSession, no cookies ───────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Custom 401 / 403 JSON response handlers ───────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))

            // ── Authorization rules (coarse-grained — fine-grained via @PreAuthorize) ──
            .authorizeHttpRequests(auth -> auth

                // Swagger UI — always public
                .requestMatchers(SWAGGER_PATHS).permitAll()

                // Auth endpoints — public (login + refresh require no token; logout is secured below)
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

                // ADMIN-only: create doctors and patients (creates User accounts too)
                .requestMatchers(HttpMethod.POST, "/api/v1/doctors").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/patients").hasRole("ADMIN")

                // ADMIN-only: deactivate (soft delete) doctors and patients
                .requestMatchers(HttpMethod.DELETE, "/api/v1/doctors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/patients/**").hasRole("ADMIN")

                // ADMIN-only: hospital settings (GET + PUT both restricted — PATIENT/DOCTOR must receive 403)
                .requestMatchers(HttpMethod.GET, "/api/v1/settings/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/settings/**").hasRole("ADMIN")

                // ADMIN + PATIENT only: invoice PDF preview and download
                // DOCTOR is explicitly excluded — invoices are patient-facing financial documents
                .requestMatchers(HttpMethod.GET, "/api/v1/invoices/**").hasAnyRole("ADMIN", "PATIENT")

                // ADMIN-only: book appointments on behalf of patients
                .requestMatchers(HttpMethod.POST, "/api/v1/appointments").hasRole("ADMIN")

                // ADMIN + DOCTOR: create lab orders and manage lab report results
                // PATIENT is explicitly excluded from write operations
                .requestMatchers(HttpMethod.POST, "/api/v1/lab-orders").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT,  "/api/v1/lab-orders/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/lab-reports").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT,  "/api/v1/lab-reports/**").hasAnyRole("ADMIN", "DOCTOR")

                // ADMIN + DOCTOR: start a consultation (POST) and modify draft/complete (PUT)
                // PATIENT is explicitly excluded — clinical records are created by doctors only
                .requestMatchers(HttpMethod.POST, "/api/v1/consultations/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT,  "/api/v1/consultations/**").hasAnyRole("ADMIN", "DOCTOR")

                // ADMIN + DOCTOR: complete or cancel appointments
                .requestMatchers(HttpMethod.PUT, "/api/v1/appointments/**").hasAnyRole("ADMIN", "DOCTOR")

                // ADMIN + DOCTOR: update doctor/patient profiles
                .requestMatchers(HttpMethod.PUT, "/api/v1/doctors/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/patients/**").hasAnyRole("ADMIN", "DOCTOR")

                // All GET endpoints and auth/logout require any authenticated user
                .requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

                // Fallback: any other request must be authenticated
                .anyRequest().authenticated()
            )

            // ── DaoAuthenticationProvider wired with our UserDetailsService ───
            .authenticationProvider(authenticationProvider())

            // ── JWT filter runs BEFORE the standard username+password filter ──
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("[SecurityConfig] Security filter chain configured successfully");
        return http.build();
    }

    // ─── Beans ────────────────────────────────────────────────────────────────

    /**
     * BCrypt password encoder with strength 12.
     * Strength 10 is the old default; 12 is more appropriate for 2025+ hardware.
     * Higher strength = more hashing rounds = slower brute force, but also slower logins.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * DaoAuthenticationProvider connects Spring Security's authentication mechanism
     * to our UserDetailsService (DB lookup) and PasswordEncoder (BCrypt comparison).
     *
     * Spring Security 7.0 removed the no-arg constructor — UserDetailsService must be
     * supplied via the constructor, not via setUserDetailsService().
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7.0+: pass UserDetailsService through constructor (no-arg ctor removed)
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        log.debug("[SecurityConfig] DaoAuthenticationProvider configured with BCrypt(12) + UserDetailsServiceImpl");
        return provider;
    }

    /**
     * Prevents Spring Boot from auto-registering JwtAuthenticationFilter as a
     * plain servlet filter (which would run it twice per request).
     *
     * Problem: JwtAuthenticationFilter is @Component, so Spring Boot's
     * FilterRegistrationAutoConfiguration registers it at the servlet level.
     * SecurityConfig also registers it via addFilterBefore() in the Spring Security
     * filter chain. Without this bean, the filter executes twice on every request.
     *
     * Fix: disable the servlet-level registration; Spring Security manages it exclusively.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setEnabled(false); // Spring Security handles registration via addFilterBefore()
        log.debug("[SecurityConfig] JwtAuthenticationFilter servlet auto-registration disabled");
        return registration;
    }

    /**
     * Exposes the AuthenticationManager bean for injection into AuthServiceImpl.
     * The AuthenticationManager is used internally by DaoAuthenticationProvider.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

}
