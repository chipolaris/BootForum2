package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the data required for a user login attempt.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
    // No explicit constructor, getters, equals, hashCode, or toString needed.
    // They are automatically generated.
}