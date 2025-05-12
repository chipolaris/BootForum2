package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public record ForumMapDTO(List<ForumDTO> forums, List<ForumGroupDTO> forumGroups) {
}
