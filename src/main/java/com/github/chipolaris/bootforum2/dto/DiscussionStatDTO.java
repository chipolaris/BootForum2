package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DiscussionStatDTO(
        Long commentCount,
        Long viewCount,
        LocalDateTime lastViewed,
        Long imageCount,
        Long attachmentCount,
        CommentInfoDTO lastComment,
        Map<String, Integer> participants,
        Integer voteUpCount,
        Integer voteDownCount) {
}
