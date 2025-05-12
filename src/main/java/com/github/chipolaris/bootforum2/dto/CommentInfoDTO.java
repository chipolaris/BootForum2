package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.CommentInfo;

import java.time.LocalDateTime;

public record CommentInfoDTO(Long id, String title, String contentAbbr, Long commentId, String commentor, LocalDateTime commentDate) {

    /* CommentInfo is a computed entity. Data is not taken from the DTO. So no need to have the toCommentInfo() method */

    // create a new DTO from the entity object
    public static CommentInfoDTO fromCommentInfo(CommentInfo commentInfo) {

        return commentInfo == null ? null : new CommentInfoDTO(commentInfo.getId(), commentInfo.getTitle(),
                commentInfo.getContentAbbr(), commentInfo.getCommentId(),
                commentInfo.getCommentor(), commentInfo.getCommentDate());
    }
}
