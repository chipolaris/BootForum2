package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record DiscussionSummaryDTO(Long id, String title, long commentCount, long viewCount, LocalDateTime createDate, String createBy, LocalDateTime lastCommentDate) {
}
