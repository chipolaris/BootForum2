package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentDTO(
        Long id,
        LocalDateTime createDate,
        String createBy,
        LocalDateTime updateDate,
        String updateBy,
        String title,
        String content,
        // CommentDTO replyToId, // don't use this in DTO, could cause a circular reference
        Long replyToId,
        // List<CommentDTO> replies, // Don't need this as we typically use for flat list of comments
        String ipAddress,
        List<FileInfoDTO> attachments,
        List<FileInfoDTO> images,
        boolean hidden,
        CommentVoteDTO commentVote
        // The 'discussion' attribute is intentionally omitted here
) {}