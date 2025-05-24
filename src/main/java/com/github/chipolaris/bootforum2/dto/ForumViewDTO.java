package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public record ForumViewDTO(ForumDTO forumDTO, List<DiscussionDTO> discussionDTOs) {

}
