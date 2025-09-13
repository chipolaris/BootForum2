package com.github.chipolaris.bootforum2.dto.admin;

import java.util.List;

public record ChartDataDTO(List<String> labels, List<ChartDataSetDTO> datasets) {
}