package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record UserProfileDiscussionDTO(Long discussionId, String discussionTitle, LocalDateTime createdDate,
                                       String title, String content) {
}
