package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record SystemStatisticDTO(Long userCount, Long forumCount, Long discussionCount,
                                 Long commentCount, String lastRegisteredUser, LocalDateTime lastUserRegisteredDate,
                                 CommentInfoDTO lastComment, DiscussionInfoDTO lastDiscussion) {
}
