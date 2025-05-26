package com.github.chipolaris.bootforum2.dto;

import java.util.Set;

public record CommentVoteDTO(
        Long id,
        int voteUpCount,
        int voteDownCount,
        Set<VoteDTO> votes // Represents the collection of individual votes
) {
}