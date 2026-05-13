package com.mediflow.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound DTO for POST /api/v1/auth/login.
 *
 * The 'usernameOrEmail' field accepts either the username OR the email address.
 * The service resolves the actual User by trying both lookups in sequence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
}
