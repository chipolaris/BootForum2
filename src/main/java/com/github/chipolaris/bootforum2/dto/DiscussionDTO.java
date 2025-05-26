package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.DiscussionStat;

import java.time.LocalDateTime;
import java.util.List;

public record DiscussionDTO(Long id, LocalDateTime createDate, String createBy, String title, List<TagDTO> tags, DiscussionStatDTO stat) {
}
