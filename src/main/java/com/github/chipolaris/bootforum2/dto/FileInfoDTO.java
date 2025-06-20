package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FileInfoDTO(
        Long id,

        @NotBlank
        @Size(max = 200)
        String originalFilename,

        @NotBlank
        @Size(max = 100)
        String mimeType,

        Long fileSize
) {
}