package com.mediflow.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Outbound DTO returned by POST /api/v1/auth/login and POST /api/v1/auth/refresh.
 *
 * Fields:
 * - accessToken  : short-lived JWT (1 hour) — include as "Authorization: Bearer <token>" header.
 * - refreshToken : long-lived opaque token (7 days) — store securely, use only for /refresh.
 * - tokenType    : always "Bearer" per RFC 6750.
 * - expiresIn    : access token TTL in seconds (3600 = 1 hour) — lets frontend schedule refresh.
 * - roles        : granted roles so frontend can adjust UI without parsing the JWT.
 * - username     : logged-in user's username for display purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Access token lifetime in seconds. */
    private long expiresIn;

    private List<String> roles;
    private String username;
}
