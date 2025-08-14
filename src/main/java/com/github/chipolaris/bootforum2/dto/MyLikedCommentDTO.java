package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record MyLikedCommentDTO(Long id, String title, String author, LocalDateTime likeDate, Long discussionId, String discussionTitle) {
}