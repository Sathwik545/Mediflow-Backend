package com.mediflow.platform.auth.filter;

import tools.jackson.databind.ObjectMapper;
import com.mediflow.platform.auth.jwt.JwtUtil;
import com.mediflow.platform.auth.security.UserDetailsServiceImpl;
import com.mediflow.platform.auth.security.UserPrincipal;
import com.mediflow.platform.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter — runs once per request, before the Spring Security
 * UsernamePasswordAuthenticationFilter.
 *
 * Responsibility:
 *  1. Extract the Bearer token from the Authorization header.
 *  2. Validate token signature, expiry, and structure via JwtUtil.
 *  3. Load the UserDetails from the DB (catches account status changes post-issuance).
 *  4. Verify account is ACTIVE and not LOCKED.
 *  5. Populate the SecurityContextHolder so downstream code sees an authenticated principal.
 *  6. (Optional) Set X-New-Token header if the token is close to expiry — sliding session hint.
 *
 * On any failure:
 *  - Writes a structured 401 JSON response directly (does NOT throw; stays in filter chain).
 *  - SecurityContext remains empty, so any secured endpoint returns 403 via AccessDeniedHandler.
 *
 * Public endpoints (/api/v1/auth/**, Swagger) are excluded by the SecurityFilterChain's
 * permitAll() rules — this filter still runs but sets no authentication, which is fine.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 15-minute window before expiry triggers a proactive refresh header
    private static final long REFRESH_HINT_THRESHOLD_MS = 15 * 60 * 1_000L;

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        log.debug("[JwtFilter] Processing request: {} {}", request.getMethod(), requestUri);

        // 1. Extract Bearer token from Authorization header
        String token = extractBearerToken(request);

        if (token == null) {
            // No token — proceed without authentication (public routes will pass; secured routes get 403)
            log.debug("[JwtFilter] No Bearer token found for request: {}", requestUri);
            chain.doFilter(request, response);
            return;
        }

        // 2. Validate token structure, signature, and expiry
        if (!jwtUtil.validateToken(token)) {
            log.warn("[JwtFilter] Invalid or expired JWT for request: {}", requestUri);
            sendUnauthorizedResponse(response, "Session expired. Please login again.");
            return;
        }

        // 3. Extract identity from validated token
        String username = jwtUtil.extractUsername(token);
        Long   userId   = jwtUtil.extractUserId(token);
        log.debug("[JwtFilter] Valid JWT for username='{}' userId={}", username, userId);

        // 4. Load UserDetails from DB — ensures real-time account status check
        UserPrincipal principal;
        try {
            principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
        } catch (Exception ex) {
            log.warn("[JwtFilter] User '{}' not found in DB — JWT may belong to a deleted account", username);
            sendUnauthorizedResponse(response, "Authentication failed. Please login again.");
            return;
        }

        // 5. Enforce account status — block locked/inactive accounts even with a valid JWT
        if (!principal.isEnabled()) {
            log.warn("[JwtFilter] User '{}' is INACTIVE — blocking request", username);
            sendUnauthorizedResponse(response, "Your account has been deactivated. Please contact support.");
            return;
        }
        if (!principal.isAccountNonLocked()) {
            log.warn("[JwtFilter] User '{}' is LOCKED — blocking request", username);
            sendUnauthorizedResponse(response, "Your account is locked. Please contact the administrator.");
            return;
        }

        // 6. Populate SecurityContextHolder — no credentials needed (already validated via JWT)
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("[JwtFilter] SecurityContext set for user='{}' roles={}",
                username, principal.getAuthorities());

        // 7. Sliding-session hint: if token is within 15 min of expiry, send refresh hint header
        if (jwtUtil.isCloseToExpiry(token, REFRESH_HINT_THRESHOLD_MS)) {
            List<String> roles = jwtUtil.extractRoles(token);
            String newToken = jwtUtil.generateAccessToken(userId, username, roles);
            response.setHeader("X-New-Token", newToken);
            log.debug("[JwtFilter] Sliding-session: new token issued for user='{}' via X-New-Token header", username);
        }

        chain.doFilter(request, response);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts the JWT from "Authorization: Bearer <token>" header.
     * Returns null if the header is absent or malformed.
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Writes a 401 JSON response directly to the servlet response.
     * This bypasses the MVC exception handling since filters run outside the DispatcherServlet.
     * The response format matches the existing ApiResponse envelope for client consistency.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(message);
        String json = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(json);
    }
}
