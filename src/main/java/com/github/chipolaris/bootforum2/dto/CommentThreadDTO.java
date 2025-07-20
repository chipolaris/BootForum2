package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public record CommentThreadDTO(DiscussionDTO discussionDTO, List<CommentDTO> commentDTOs) {
}
