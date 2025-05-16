package com.github.chipolaris.bootforum2.dto;

public record ForumGroupCreateDTO(
        String title,
        String icon,
        String iconColor,
        Long parentGroupId) {
}
