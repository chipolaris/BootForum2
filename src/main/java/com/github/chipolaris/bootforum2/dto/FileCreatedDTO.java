package com.github.chipolaris.bootforum2.dto;

public record FileCreatedDTO(String originalFilename, String mimeType, Long fileSize, String path) {
}
