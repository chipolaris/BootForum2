package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagCreateDTO(
        @NotBlank @Size(max = 100) String label,
        @Size(max = 30) String icon,
        @Size(max = 30) String iconColor) {
}