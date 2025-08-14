package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record MyLikedDiscussionDTO(Long id, String title, String author, LocalDateTime likeDate) {
}