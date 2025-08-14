package com.github.chipolaris.bootforum2.dto;

import java.util.List;

public class MyActivitiesDTO {

    private List<MyRecentDiscussionDTO> recentDiscussions;
    private List<MyRecentCommentDTO> recentComments;
    private List<ReplyToMyCommentDTO> repliesToMyComments;
    private List<MyLikedDiscussionDTO> likedDiscussions;
    private List<MyLikedCommentDTO> likedComments;

    // Getters and Setters
    public List<MyRecentDiscussionDTO> getRecentDiscussions() {
        return recentDiscussions;
    }

    public void setRecentDiscussions(List<MyRecentDiscussionDTO> recentDiscussions) {
        this.recentDiscussions = recentDiscussions;
    }

    public List<MyRecentCommentDTO> getRecentComments() {
        return recentComments;
    }

    public void setRecentComments(List<MyRecentCommentDTO> recentComments) {
        this.recentComments = recentComments;
    }

    public List<ReplyToMyCommentDTO> getRepliesToMyComments() {
        return repliesToMyComments;
    }

    public void setRepliesToMyComments(List<ReplyToMyCommentDTO> repliesToMyComments) {
        this.repliesToMyComments = repliesToMyComments;
    }

    public List<MyLikedDiscussionDTO> getLikedDiscussions() {
        return likedDiscussions;
    }

    public void setLikedDiscussions(List<MyLikedDiscussionDTO> likedDiscussions) {
        this.likedDiscussions = likedDiscussions;
    }

    public List<MyLikedCommentDTO> getLikedComments() {
        return likedComments;
    }

    public void setLikedComments(List<MyLikedCommentDTO> likedComments) {
        this.likedComments = likedComments;
    }
}