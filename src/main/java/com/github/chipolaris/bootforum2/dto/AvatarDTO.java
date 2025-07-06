package com.github.chipolaris.bootforum2.dto;

/**
 * DTO for representing a user's avatar information.
 *
 * @param username The username associated with the avatar.
 * @param fileInfo The DTO containing details of the avatar file.
 */
public record AvatarDTO(
        String username,
        FileInfoDTO fileInfo
) {}