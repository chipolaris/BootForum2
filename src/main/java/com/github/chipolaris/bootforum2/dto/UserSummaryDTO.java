package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UserSummaryDTO(Long id, String username, String firstName, String lastName, Set<String> roles, String accountStatus, LocalDateTime lastLogin) {
}