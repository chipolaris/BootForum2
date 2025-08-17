package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TagUpdateDTO(
        @NotNull Long id,
        @NotBlank @Size(max = 100) String label,
        @Size(max = 30) String icon,
        @Size(max = 30) String iconColor,
        boolean disabled) {
}