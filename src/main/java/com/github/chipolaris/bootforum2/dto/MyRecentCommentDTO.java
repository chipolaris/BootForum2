package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record MyRecentCommentDTO(Long id, String title, LocalDateTime createDate, Long discussionId, String discussionTitle) {
}