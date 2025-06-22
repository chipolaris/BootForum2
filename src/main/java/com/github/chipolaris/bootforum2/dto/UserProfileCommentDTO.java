package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record UserProfileCommentDTO(Long commentId, LocalDateTime createdDate, String commentTitle,
                                    String content, Long discussionId, String discussionTitle) {
}
