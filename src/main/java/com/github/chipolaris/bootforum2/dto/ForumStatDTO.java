package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.ForumStat;

public record ForumStatDTO(Long id, CommentInfoDTO lastComment, long commentCount, long discussionCount) {

    /* ForumStat is a computed entity. Data is not taken from the DTO. So no need to have the toForumStat() method */

    //
    public static ForumStatDTO fromForumStat(ForumStat forumStat) {

        return forumStat == null ? null : new ForumStatDTO(forumStat.getId(),
                CommentInfoDTO.fromCommentInfo(forumStat.getLastComment()),
                forumStat.getCommentCount(), forumStat.getDiscussionCount());
    }
}
