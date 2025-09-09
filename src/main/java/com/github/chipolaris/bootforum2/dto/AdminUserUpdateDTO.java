package com.github.chipolaris.bootforum2.dto;

import java.util.Set;

public record AdminUserUpdateDTO(Set<String> roles, String accountStatus) {
}