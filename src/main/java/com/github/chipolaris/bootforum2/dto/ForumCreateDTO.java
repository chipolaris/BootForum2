package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumCreateDTO(
        @NotBlank
        @Size(min = 3, max = 100)
        String title,

        @NotBlank
        @Size(min = 3, max = 500)
        String description,

        @NotBlank
        @Size(max = 50)
        String icon,

        @NotBlank
        @Size(max = 50)
        String iconColor,

        boolean active,

        Long parentGroupId) {
}
