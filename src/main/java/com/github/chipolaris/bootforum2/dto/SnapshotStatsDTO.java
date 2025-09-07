package com.github.chipolaris.bootforum2.dto;

public record SnapshotStatsDTO(long memberCount, long forumCount, long discussionCount, long tagCount, long commentCount,
                               long attachmentCount, long imageCount) {
}