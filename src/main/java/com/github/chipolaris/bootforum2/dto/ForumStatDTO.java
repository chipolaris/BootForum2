package com.github.chipolaris.bootforum2.dto;

public record ForumStatDTO(Long id, CommentInfoDTO lastComment, long commentCount, long discussionCount) {
}
