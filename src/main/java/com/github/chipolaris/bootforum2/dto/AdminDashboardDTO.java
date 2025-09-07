package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public class AdminDashboardDTO {

    private SnapshotStatsDTO snapshotStats;
    private List<RankedListItemDTO> usersByDiscussions;
    private List<RankedListItemDTO> usersByComments;
    private List<RankedListItemDTO> usersByReputation;
    private List<RankedListItemDTO> discussionsByViews;
    private List<RankedListItemDTO> discussionsByComments;
    private List<RankedListItemDTO> tagsByViews;
    private List<RankedListItemDTO> tagsByComments;
    private List<RankedListItemDTO> forumsByViews;
    private List<RankedListItemDTO> forumsByComments;

    // Getters and Setters
    public SnapshotStatsDTO getSnapshotStats() {
        return snapshotStats;
    }

    public void setSnapshotStats(SnapshotStatsDTO snapshotStats) {
        this.snapshotStats = snapshotStats;
    }

    public List<RankedListItemDTO> getUsersByDiscussions() {
        return usersByDiscussions;
    }

    public void setUsersByDiscussions(List<RankedListItemDTO> usersByDiscussions) {
        this.usersByDiscussions = usersByDiscussions;
    }

    public List<RankedListItemDTO> getUsersByComments() {
        return usersByComments;
    }

    public void setUsersByComments(List<RankedListItemDTO> usersByComments) {
        this.usersByComments = usersByComments;
    }

    public List<RankedListItemDTO> getUsersByReputation() {
        return usersByReputation;
    }

    public void setUsersByReputation(List<RankedListItemDTO> usersByReputation) {
        this.usersByReputation = usersByReputation;
    }

    public List<RankedListItemDTO> getDiscussionsByViews() {
        return discussionsByViews;
    }

    public void setDiscussionsByViews(List<RankedListItemDTO> discussionsByViews) {
        this.discussionsByViews = discussionsByViews;
    }

    public List<RankedListItemDTO> getDiscussionsByComments() {
        return discussionsByComments;
    }

    public void setDiscussionsByComments(List<RankedListItemDTO> discussionsByComments) {
        this.discussionsByComments = discussionsByComments;
    }

    public List<RankedListItemDTO> getTagsByViews() {
        return tagsByViews;
    }

    public void setTagsByViews(List<RankedListItemDTO> tagsByViews) {
        this.tagsByViews = tagsByViews;
    }

    public List<RankedListItemDTO> getTagsByComments() {
        return tagsByComments;
    }

    public void setTagsByComments(List<RankedListItemDTO> tagsByComments) {
        this.tagsByComments = tagsByComments;
    }

    public List<RankedListItemDTO> getForumsByViews() {
        return forumsByViews;
    }

    public void setForumsByViews(List<RankedListItemDTO> forumsByViews) {
        this.forumsByViews = forumsByViews;
    }

    public List<RankedListItemDTO> getForumsByComments() {
        return forumsByComments;
    }

    public void setForumsByComments(List<RankedListItemDTO> forumsByComments) {
        this.forumsByComments = forumsByComments;
    }
}