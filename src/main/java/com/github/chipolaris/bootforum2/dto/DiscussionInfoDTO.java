package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record DiscussionInfoDTO(Long discussionId, String title, String contentAbbr,
                                String discussionCreator, LocalDateTime discussionCreateDate) {
}
