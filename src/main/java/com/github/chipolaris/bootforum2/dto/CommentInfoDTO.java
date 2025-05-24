package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.CommentInfo;

import java.time.LocalDateTime;

public record CommentInfoDTO(Long id, String title, String contentAbbr, Long commentId, String commentor, LocalDateTime commentDate) {
}
