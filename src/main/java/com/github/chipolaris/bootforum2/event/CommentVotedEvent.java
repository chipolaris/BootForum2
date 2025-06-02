package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Comment;
import org.springframework.context.ApplicationEvent;

public class CommentVotedEvent extends ApplicationEvent {

    private final Comment comment;
    private final String voterUsername;
    private final short voteValue;

    public CommentVotedEvent(Object source, Comment comment, String voterUsername, short voteValue) {
        super(source);
        this.comment = comment;
        this.voterUsername = voterUsername;
        this.voteValue = voteValue;
    }

    public Comment getComment() {
        return comment;
    }

    public String getVoterUsername() {
        return voterUsername;
    }

    public short getVoteValue() {
        return voteValue;
    }
}