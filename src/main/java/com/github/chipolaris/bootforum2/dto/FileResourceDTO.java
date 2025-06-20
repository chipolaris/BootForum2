package com.github.chipolaris.bootforum2.dto;

import org.springframework.core.io.Resource;

public record FileResourceDTO (Resource resource, String originalFilename, String mimeType) {

}