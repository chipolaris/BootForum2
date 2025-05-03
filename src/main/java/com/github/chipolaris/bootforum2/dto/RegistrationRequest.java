package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents the data required for a user registration attempt.
 */
public record RegistrationRequest(
        @NotBlank
        @Size(min = 3, max = 20)
        String username,

        @NotBlank
        @Size(min = 6, max = 40)
        String password,

        // Note: confirmPassword is validated on the frontend, not sent here

        @NotBlank
        @Size(max = 50)
        String firstName,

        @NotBlank
        @Size(max = 50)
        String lastName,

        @NotBlank
        @Size(max = 50)
        @Email
        String email
) {
    // No explicit constructor, getters, setters, equals, hashCode, or toString needed.
}