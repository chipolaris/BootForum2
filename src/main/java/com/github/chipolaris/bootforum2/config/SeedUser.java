// SeedUser.java (as a record)
package com.github.chipolaris.bootforum2.config;

import com.github.chipolaris.bootforum2.enumeration.AccountStatus;
import com.github.chipolaris.bootforum2.enumeration.UserRole;

public record SeedUser(
        String username,
        String password,
        UserRole userRole,
        AccountStatus accountStatus,
        SeedPerson person
) {
}