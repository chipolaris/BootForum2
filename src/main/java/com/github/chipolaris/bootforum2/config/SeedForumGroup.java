// SeedForumGroup.java
package com.github.chipolaris.bootforum2.config;

public record SeedForumGroup(
        String title,
        String icon,
        String iconColor,
        Integer sortOrder // Optional, can be null
        // Add parentTitle if you need to link to a parent by title,
        // but for the root, parent will be null.
) {
}