package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileDTO(String username, String firstName, String lastName, LocalDateTime joinDate,
                             Long discussionCreatedCount, Long commentCount, Long imageUploaded, Long attachmentUploaded,
                             Long reputation, Long profileViewed, LocalDateTime lastLogin,
                             List<UserProfileCommentDTO> comments, List<UserProfileDiscussionDTO> discussions) {
}
