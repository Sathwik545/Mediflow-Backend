package com.mediflow.platform.auth.handler;

import tools.jackson.databind.ObjectMapper;
import com.mediflow.platform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 401 Unauthorized responses for requests that reach a secured endpoint
 * without a valid authentication (no JWT, or JWT rejected by the filter chain).
 *
 * Spring Security invokes this when SecurityContextHolder has no authentication
 * and the resource requires authentication. Without a custom entry point,
 * Spring returns an HTML error page; this replaces it with our ApiResponse JSON envelope.
 *
 * Note: 401 from an EXPIRED token is caught earlier in JwtAuthenticationFilter
 * and written directly, so this handler covers cases like missing token on secured routes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("[AuthEntryPoint] Unauthorized access attempt: {} {} — {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(
            "Authentication required. Please provide a valid Bearer token."
        );
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
