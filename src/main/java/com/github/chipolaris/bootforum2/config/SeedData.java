// SeedData.java (as a record)
package com.github.chipolaris.bootforum2.config;

import java.util.List;

public record SeedData(List<SeedUser> users, List<SeedForumGroup> forumGroups) {
}
