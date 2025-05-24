package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.DiscussionStat;

import java.util.List;

public record DiscussionDTO(Long id, String title, List<TagDTO> tags, DiscussionStatDTO stat) {
}
