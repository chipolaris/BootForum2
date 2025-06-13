package com.github.chipolaris.bootforum2.dto;

import java.time.LocalDateTime;

public record PersonDTO(Long id, String firstName, String lastName, String email,
                        LocalDateTime createDate, LocalDateTime updateDate,
                        String createdBy, String updatedBy) {
}
