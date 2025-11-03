package com.github.chipolaris.bootforum2.dto;

import java.util.Map;

/**
 * Represents the health status of a single application component (e.g., database, disk space).
 */
public record HealthStatusDTO(String component, String status, Map<String, Object> details) {
}