package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

// Assuming Vote entity has at least these fields
public record VoteDTO(
        Long id,
        String voter, // or UserDTO if you have one
        LocalDateTime voteDate,
        String voteType // e.g., "UP", "DOWN"
) {
}