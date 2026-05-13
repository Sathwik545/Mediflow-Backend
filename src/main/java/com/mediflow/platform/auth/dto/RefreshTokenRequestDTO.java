package com.mediflow.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Inbound DTO for POST /api/v1/auth/refresh — carries the long-lived refresh token. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
