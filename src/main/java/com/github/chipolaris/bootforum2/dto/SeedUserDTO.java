package com.github.chipolaris.bootforum2.dto;

public record SeedUserDTO(String username, String password, String userRole, String accountStatus, SeedPersonDTO person) {
}
