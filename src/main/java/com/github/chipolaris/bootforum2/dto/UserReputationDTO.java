package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public class UserReputationDTO {

    // Summary Stats
    private long profileViewCount;
    private long totalDiscussions;
    private long totalComments;
    private long totalUpVotes;
    private long totalDownVotes;

    // Ranked Lists
    private List<RankedDiscussionDTO> mostViewedDiscussions;
    private List<RankedDiscussionDTO> mostLikedDiscussions;
    private List<RankedDiscussionDTO> mostDislikedDiscussions;
    private List<RankedDiscussionDTO> mostNetLikedDiscussions;
    private List<RankedCommentDTO> mostLikedComments;
    private List<RankedCommentDTO> mostDislikedComments;

    // Getters and Setters
    public long getProfileViewCount() {
        return profileViewCount;
    }

    public void setProfileViewCount(long profileViewCount) {
        this.profileViewCount = profileViewCount;
    }

    public long getTotalDiscussions() {
        return totalDiscussions;
    }

    public void setTotalDiscussions(long totalDiscussions) {
        this.totalDiscussions = totalDiscussions;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    public long getTotalUpVotes() {
        return totalUpVotes;
    }

    public void setTotalUpVotes(long totalUpVotes) {
        this.totalUpVotes = totalUpVotes;
    }

    public long getTotalDownVotes() {
        return totalDownVotes;
    }

    public void setTotalDownVotes(long totalDownVotes) {
        this.totalDownVotes = totalDownVotes;
    }

    public List<RankedDiscussionDTO> getMostViewedDiscussions() {
        return mostViewedDiscussions;
    }

    public void setMostViewedDiscussions(List<RankedDiscussionDTO> mostViewedDiscussions) {
        this.mostViewedDiscussions = mostViewedDiscussions;
    }

    public List<RankedDiscussionDTO> getMostLikedDiscussions() {
        return mostLikedDiscussions;
    }

    public void setMostLikedDiscussions(List<RankedDiscussionDTO> mostLikedDiscussions) {
        this.mostLikedDiscussions = mostLikedDiscussions;
    }

    public List<RankedDiscussionDTO> getMostDislikedDiscussions() {
        return mostDislikedDiscussions;
    }

    public void setMostDislikedDiscussions(List<RankedDiscussionDTO> mostDislikedDiscussions) {
        this.mostDislikedDiscussions = mostDislikedDiscussions;
    }

    public List<RankedDiscussionDTO> getMostNetLikedDiscussions() {
        return mostNetLikedDiscussions;
    }

    public void setMostNetLikedDiscussions(List<RankedDiscussionDTO> mostNetLikedDiscussions) {
        this.mostNetLikedDiscussions = mostNetLikedDiscussions;
    }

    public List<RankedCommentDTO> getMostLikedComments() {
        return mostLikedComments;
    }

    public void setMostLikedComments(List<RankedCommentDTO> mostLikedComments) {
        this.mostLikedComments = mostLikedComments;
    }

    public List<RankedCommentDTO> getMostDislikedComments() {
        return mostDislikedComments;
    }

    public void setMostDislikedComments(List<RankedCommentDTO> mostDislikedComments) {
        this.mostDislikedComments = mostDislikedComments;
    }
}