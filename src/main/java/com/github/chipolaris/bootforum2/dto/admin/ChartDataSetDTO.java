package com.github.chipolaris.bootforum2.dto.admin;

import java.util.List;

public record ChartDataSetDTO(String label, List<Number> data) {
}