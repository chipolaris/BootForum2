package com.github.chipolaris.bootforum2.dto.admin;

/**
 * Configuration record for simulating discussions.
 * @param numberOfForumGroups The total number of top-level forum groups to create.
 * @param minForumsPerGroup The minimum number of forums to create in each group.
 * @param maxForumsPerGroup The maximum number of forums to create in each group.
 * @param minDiscussionsPerForum The minimum number of discussions to create in each forum.
 * @param maxDiscussionsPerForum The maximum number of discussions to create in each forum.
 * @param minCommentsPerDiscussion The minimum number of comments to create in each discussion.
 * @param maxCommentsPerDiscussion The maximum number of comments to create in each discussion.
 */
public record DiscussionSimulationConfigDTO(
        int numberOfForumGroups,
        int minForumsPerGroup,
        int maxForumsPerGroup,
        int minDiscussionsPerForum,
        int maxDiscussionsPerForum,
        int minCommentsPerDiscussion,
        int maxCommentsPerDiscussion
) {}