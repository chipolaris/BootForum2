package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record MyRecentDiscussionDTO(Long id, String title, LocalDateTime createDate, String lastReplyTitle, LocalDateTime lastReplyDate) {
}