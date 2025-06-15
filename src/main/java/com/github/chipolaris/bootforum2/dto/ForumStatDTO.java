package com.github.chipolaris.bootforum2.dto;

public record ForumStatDTO(Long id, CommentInfoDTO lastComment, DiscussionInfoDTO lastDiscussion, long commentCount, long discussionCount) {
}
