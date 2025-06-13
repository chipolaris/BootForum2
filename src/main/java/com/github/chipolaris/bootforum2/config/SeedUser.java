// SeedUser.java (as a record)
package com.github.chipolaris.bootforum2.config;

import com.github.chipolaris.bootforum2.enumeration.AccountStatus;
import com.github.chipolaris.bootforum2.enumeration.UserRole;

import java.util.Set;

public record SeedUser(
        String username,
        String password,
        Set<UserRole> userRoles,
        AccountStatus accountStatus,
        SeedPerson person
) {
}