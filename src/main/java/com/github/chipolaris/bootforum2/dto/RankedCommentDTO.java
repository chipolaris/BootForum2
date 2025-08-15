package com.github.chipolaris.bootforum2.dto;

public record RankedCommentDTO(Long id, String title, Long value, Long discussionId, String discussionTitle) {
}