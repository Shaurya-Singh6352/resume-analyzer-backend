package com.resumeanalyzer.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Groups the small request/response records used by the auth endpoints. */
public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Enter your name")
            @Size(max = 100, message = "Name is too long")
            String fullName,

            @NotBlank(message = "Enter your email address")
            @Email(message = "Enter a valid email address")
            String email,

            // BCrypt only uses the first 72 bytes, so we cap the length there
            @NotBlank(message = "Enter a password")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Enter your email address") String email,
            @NotBlank(message = "Enter your password") String password
    ) {}

    public record AuthResponse(String token, Long userId, String fullName, String email) {}
}