package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DiscussionStatDTO(
        Long commentCount,
        Long viewCount,
        LocalDateTime lastViewed,
        Long thumbnailCount,
        Long attachmentCount,
        CommentInfoDTO lastComment,
        Map<String, Integer> commentors) {
}
