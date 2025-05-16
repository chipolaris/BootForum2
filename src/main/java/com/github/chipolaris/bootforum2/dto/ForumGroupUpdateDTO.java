package com.github.chipolaris.bootforum2.dto;

public record ForumGroupUpdateDTO(
        Long id,
        String title,
        String icon,
        String iconColor
    ) {
}
