package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeDTO(
        @NotBlank
        String oldPassword,

        @NotBlank
        @Size(min = 8, message = "New password must be at least 8 characters long")
        String newPassword
) {}