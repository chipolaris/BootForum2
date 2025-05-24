package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.ForumStat;

public record ForumStatDTO(Long id, CommentInfoDTO lastComment, long commentCount, long discussionCount) {
}
