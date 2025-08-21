package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public record SettingDTO(
        String key,
        String label,
        String type,
        Object value,
        List<String> options
) {}
