package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record ReplyToMyCommentDTO(Long replyId, String replyTitle, LocalDateTime replyDate, String replyAuthor,
                                  Long myCommentId, String myCommentTitle, Long discussionId, String discussionTitle) {
}