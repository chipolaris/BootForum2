package com.github.chipolaris.bootforum2.dto;

import java.util.List;

/**
 * Represents the overall health of the application, including its sub-components.
 */
public record ApplicationHealthDTO(String overallStatus, List<HealthStatusDTO> components) {
}