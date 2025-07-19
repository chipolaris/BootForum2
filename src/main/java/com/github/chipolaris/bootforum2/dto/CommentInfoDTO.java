package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.CommentInfo;

import java.time.LocalDateTime;

public record CommentInfoDTO(Long commentId, String title, String contentAbbr,
                             String commentor, LocalDateTime commentDate,
                             Long discussionId, String discussionTitle) {
}
