package com.github.chipolaris.bootforum2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new discussion.
 * Corresponds to the textual parts of the Angular DiscussionCreateDTO.
 * File parts (images, attachments) are typically handled as MultipartFile[] in the controller.
 */
public record DiscussionCreateDTO(
        @NotNull(message = "Forum ID cannot be null")
        Long forumId,

        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        @NotBlank(message = "Discussion content cannot be blank")
        String content // This will be the content for the first comment in the discussion
) {
}