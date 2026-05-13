package com.mediflow.platform.auth.handler;

import tools.jackson.databind.ObjectMapper;
import com.mediflow.platform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 403 Forbidden responses when an authenticated user attempts to access
 * a resource that requires a role they do not hold.
 *
 * Example: a PATIENT user calling POST /api/v1/doctors (which requires ADMIN role).
 *
 * Without this handler, Spring Security returns an HTML Whitelabel error page.
 * This replaces it with the standard ApiResponse JSON envelope.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "anonymous";

        log.warn("[AccessDenied] User '{}' attempted unauthorized access: {} {}",
                username, request.getMethod(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(
            "Access denied. You do not have the required permissions to perform this action."
        );
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
