package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record UserDTO(Long id, String username, String[] userRoles,
                      String accountStatus, PersonDTO person, LocalDateTime createDate,
                      LocalDateTime updateDate, String createdBy, String updatedBy) {
}
